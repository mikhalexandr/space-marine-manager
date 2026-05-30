package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.request.payload.CommandPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.db.Database;
import dev.mikhalexandr.server.db.IdempotencyRecord;
import dev.mikhalexandr.server.db.IdempotencyStore;
import dev.mikhalexandr.server.exceptions.DuplicateRequestException;
import dev.mikhalexandr.server.exceptions.IdempotencyConflictException;
import dev.mikhalexandr.server.managers.CommandExecutor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Обеспечивает идемпотентность изменяющих команд через таблицу {@code idempotency_keys} в той же
 * постгре
 */
public final class IdempotencyInterceptor implements CommandInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyInterceptor.class);

  private static final Set<CommandType> MUTATING_COMMANDS =
      EnumSet.of(
          CommandType.ADD,
          CommandType.UPDATE,
          CommandType.REMOVE_BY_ID,
          CommandType.CLEAR,
          CommandType.ADD_IF_MIN);

  private static final int MAX_ATTEMPTS = 5;
  private static final String ANONYMOUS = "strannij_chel";

  private final Database database;
  private final IdempotencyStore store;

  public IdempotencyInterceptor(Database database, IdempotencyStore store) {
    this.database = database;
    this.store = store;
  }

  @Override
  public CommandResponse intercept(CommandRequest request, CommandExecutor next) {
    String requestId = request.getRequestId();
    if (requestId == null || requestId.isBlank() || !isMutating(request)) {
      return next.execute(request);
    }

    String userId = resolveUserId(request);
    String requestHash = hashOf(request);

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      try {
        return database.inTransaction(
            connection -> {
              store.claim(userId, requestId, requestHash);
              CommandResponse response = next.execute(request);
              store.complete(userId, requestId, response);
              return response;
            });
      } catch (DuplicateRequestException duplicate) {
        Optional<CommandResponse> existing = readExisting(userId, requestId, requestHash);
        if (existing.isPresent()) {
          LOGGER.debug("Идемпотентный повтор запроса {} - возвращён сохранённый ответ", requestId);
          return existing.get();
        }
        LOGGER.debug("Ключ {} освободился, повтор попытки обработки", requestId);
      }
    }
    throw new IdempotencyConflictException("Не удалось обработать запрос идемпотентно, отдыхай");
  }

  /**
   * Возвращает уже сохранённый ответ по ключу, если он есть и готов; {@link Optional#empty()}, если
   * ключ освободился и нужна повторная попытка
   *
   * @throws IdempotencyConflictException если requestId переиспользован с другим payload
   */
  private Optional<CommandResponse> readExisting(
      String userId, String requestId, String requestHash) {
    Optional<IdempotencyRecord> record = store.find(userId, requestId);
    if (record.isEmpty()) {
      return Optional.empty();
    }
    IdempotencyRecord found = record.get();
    if (!found.requestHash().equals(requestHash)) {
      throw new IdempotencyConflictException(
          "братан, requestId уже использован с другим запросом - повтор отклонён");
    }
    if (!found.isDone() || found.response() == null) {
      throw new IdempotencyConflictException("Запрос ещё обрабатывается, зачилься");
    }
    return Optional.of(found.response());
  }

  private static boolean isMutating(CommandRequest request) {
    return MUTATING_COMMANDS.contains(request.getCommandType());
  }

  private static String resolveUserId(CommandRequest request) {
    UserCredentials credentials = request.getCredentials();
    if (credentials == null || credentials.login() == null || credentials.login().isBlank()) {
      return ANONYMOUS;
    }
    return credentials.login().trim();
  }

  /** Хэш по типу команды и payload */
  private static String hashOf(CommandRequest request) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(request.getCommandType().name().getBytes(StandardCharsets.UTF_8));
      CommandPayload payload = request.getPayload();
      if (payload != null) {
        digest.update(Serializer.serialize(payload));
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException | IOException e) {
      throw new IllegalStateException("Не удалось вычислить хэш запроса, анлак", e);
    }
  }

  /**
   * Запускает фоновую очистку устаревших записей идемпотентности
   *
   * @param retention срок хранения записей
   * @param period период запуска очистки
   * @return запущенный планировщик
   */
  public ScheduledExecutorService startCleanup(Duration retention, Duration period) {
    ThreadFactory factory =
        runnable -> {
          Thread thread = new Thread(runnable, "idempotency-cleanup");
          thread.setDaemon(true);
          return thread;
        };
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(factory);
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            int removed = store.deleteExpired(retention);
            if (removed > 0) {
              LOGGER.info("Очистка идемпотентности: удалено записей - {}", removed);
            }
          } catch (RuntimeException e) {
            LOGGER.warn("Очистка идемпотентности не удалась: {}", e.getMessage());
          }
        },
        period.toMillis(),
        period.toMillis(),
        TimeUnit.MILLISECONDS);
    return scheduler;
  }
}
