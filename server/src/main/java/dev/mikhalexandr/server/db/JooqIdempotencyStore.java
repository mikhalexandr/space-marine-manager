package dev.mikhalexandr.server.db;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.exceptions.DataAccessException;
import dev.mikhalexandr.server.exceptions.DuplicateRequestException;
import java.io.IOException;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class JooqIdempotencyStore implements IdempotencyStore {
  private static final String UNIQUE_VIOLATION = "23505";
  private static final String LOCK_NOT_AVAILABLE = "55P03";

  private static final Table<Record> IDEMPOTENCY_KEYS = table(name("idempotency_keys"));
  private static final Field<String> USER_ID = field(name("user_id"), SQLDataType.VARCHAR);
  private static final Field<String> REQUEST_ID = field(name("request_id"), SQLDataType.VARCHAR);
  private static final Field<String> REQUEST_HASH =
      field(name("request_hash"), SQLDataType.VARCHAR);
  private static final Field<String> STATUS = field(name("status"), SQLDataType.VARCHAR);
  private static final Field<byte[]> RESPONSE = field(name("response"), SQLDataType.BLOB);
  private static final Field<LocalDateTime> CREATED_AT =
      field(name("created_at"), SQLDataType.LOCALDATETIME);
  private static final Field<LocalDateTime> COMPLETED_AT =
      field(name("completed_at"), SQLDataType.LOCALDATETIME);

  private final Database database;

  public JooqIdempotencyStore(Database database) {
    this.database = database;
  }

  @Override
  public void claim(String userId, String requestId, String requestHash) {
    database.execute(connection -> doClaim(connection, userId, requestId, requestHash));
  }

  private static Void doClaim(
      Connection connection, String userId, String requestId, String requestHash) {
    try {
      DSL.using(connection, SQLDialect.POSTGRES)
          .insertInto(IDEMPOTENCY_KEYS)
          .set(USER_ID, userId)
          .set(REQUEST_ID, requestId)
          .set(REQUEST_HASH, requestHash)
          .set(STATUS, IdempotencyRecord.STATUS_PROCESSING)
          .execute();
    } catch (org.jooq.exception.DataAccessException e) {
      String sqlState = e.sqlState();
      if (UNIQUE_VIOLATION.equals(sqlState) || LOCK_NOT_AVAILABLE.equals(sqlState)) {
        throw new DuplicateRequestException("Ключ идемпотентности уже занят: " + requestId);
      }
      throw e;
    }
    return null;
  }

  @Override
  public void complete(String userId, String requestId, CommandResponse response) {
    byte[] serialized = serialize(response);
    database.execute(
        connection -> {
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          dsl.update(IDEMPOTENCY_KEYS)
              .set(STATUS, IdempotencyRecord.STATUS_DONE)
              .set(RESPONSE, serialized)
              .set(COMPLETED_AT, LocalDateTime.now())
              .where(USER_ID.eq(userId).and(REQUEST_ID.eq(requestId)))
              .execute();
          return null;
        });
  }

  @Override
  public Optional<IdempotencyRecord> find(String userId, String requestId) {
    return database.execute(
        connection -> {
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          Record record =
              dsl.select(REQUEST_HASH, STATUS, RESPONSE)
                  .from(IDEMPOTENCY_KEYS)
                  .where(USER_ID.eq(userId).and(REQUEST_ID.eq(requestId)))
                  .fetchOne();
          if (record == null) {
            return Optional.empty();
          }
          byte[] payload = record.get(RESPONSE);
          CommandResponse response = payload == null ? null : deserialize(payload);
          return Optional.of(
              new IdempotencyRecord(record.get(REQUEST_HASH), record.get(STATUS), response));
        });
  }

  @Override
  public int deleteExpired(Duration retention) {
    LocalDateTime threshold = LocalDateTime.now().minus(retention);
    return database.execute(
        connection -> {
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          return dsl.deleteFrom(IDEMPOTENCY_KEYS).where(CREATED_AT.lt(threshold)).execute();
        });
  }

  private static byte[] serialize(CommandResponse response) {
    try {
      return Serializer.serialize(response);
    } catch (IOException e) {
      throw new DataAccessException("Не удалось сериализовать ответ для идемпотентности", e);
    }
  }

  private static CommandResponse deserialize(byte[] payload) {
    try {
      return Serializer.deserialize(payload, CommandResponse.class);
    } catch (IOException | ClassNotFoundException e) {
      throw new DataAccessException("Не удалось десериализовать сохранённый ответ", e);
    }
  }
}
