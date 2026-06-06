package dev.mikhalexandr.client.gateway;

import dev.mikhalexandr.client.commands.CommandRequestParser;
import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.client.script.ScriptSpaceMarineReader;
import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.event.CollectionEvent;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.request.payload.CategoryPayload;
import dev.mikhalexandr.common.dto.request.payload.CommandPayload;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.IdMarinePayload;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.request.payload.MarinePayload;
import dev.mikhalexandr.common.dto.request.payload.NoArgsPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.common.util.Serializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Scanner;
import programming.lab8.gui.api.CollectionGateway;
import programming.lab8.gui.api.CollectionListener;
import programming.lab8.gui.api.GatewayResult;

public final class SpaceMarineGateway implements CollectionGateway<SpaceMarine> {
  private static final int MAX_SCRIPT_DEPTH = 3;

  private final TcpClient tcpClient;
  private final CommandRequestParser parser = new CommandRequestParser();
  private final ScriptSpaceMarineReader scriptSpaceMarineReader = new ScriptSpaceMarineReader();

  private final IdempotencyKeyCache idempotencyKeys = new IdempotencyKeyCache();

  public SpaceMarineGateway(TcpClient tcpClient) {
    this.tcpClient = tcpClient;
  }

  @Override
  public GatewayResult login(String username, String password) throws Exception {
    return authenticate(CommandType.LOGIN, username, password);
  }

  @Override
  public GatewayResult register(String username, String password) throws Exception {
    return authenticate(CommandType.REGISTER, username, password);
  }

  @Override
  public void subscribe(CollectionListener<SpaceMarine> listener) {
    if (listener == null) {
      tcpClient.setEventListener(null);
      return;
    }
    tcpClient.setEventListener(event -> dispatchEvent(listener, event));
  }

  private static void dispatchEvent(
      CollectionListener<SpaceMarine> listener, CollectionEvent event) {
    switch (event.getType()) {
      case ADDED -> listener.onAdded(event.getMarine());
      case UPDATED -> listener.onUpdated(event.getMarine());
      case REMOVED -> event.getRemovedIds().forEach(listener::onRemoved);
      default -> throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
    }
  }

  @Override
  public List<SpaceMarine> loadAll() throws Exception {
    CommandResponse response = send(CommandType.SHOW, NoArgsPayload.INSTANCE);
    List<SpaceMarine> data = response.getData();
    return data == null ? List.of() : data;
  }

  @Override
  public GatewayResult add(SpaceMarine object) throws Exception {
    return toResult(send(CommandType.ADD, new MarinePayload(object)));
  }

  @Override
  public GatewayResult update(long id, SpaceMarine object) throws Exception {
    return toResult(send(CommandType.UPDATE, new IdMarinePayload((int) id, object)));
  }

  @Override
  public GatewayResult remove(long id) throws Exception {
    return toResult(send(CommandType.REMOVE_BY_ID, new IdPayload((int) id)));
  }

  public CommandResponse execute(CommandType type) throws Exception {
    return execute(type, NoArgsPayload.INSTANCE);
  }

  public CommandResponse execute(CommandType type, CommandPayload payload) throws Exception {
    return send(type, payload);
  }

  public GatewayResult addIfMin(SpaceMarine object) throws Exception {
    return toResult(send(CommandType.ADD_IF_MIN, new MarinePayload(object)));
  }

  public GatewayResult countByCategory(AstartesCategory category) throws Exception {
    return toResult(send(CommandType.COUNT_BY_CATEGORY, new CategoryPayload(category)));
  }

  public GatewayResult executeScript(Path scriptPath) throws Exception {
    List<String> output = new ArrayList<>();
    runScript(scriptPath.toAbsolutePath().normalize(), new ArrayDeque<>(), output);
    String message =
        output.isEmpty() ? "Скрипт выполнен" : String.join(System.lineSeparator(), output);
    return GatewayResult.success(message);
  }

  private GatewayResult authenticate(CommandType type, String username, String password)
      throws Exception {
    UserCredentials credentials = new UserCredentials(username, password);
    CommandRequest request = new CommandRequest(type, NoArgsPayload.INSTANCE);
    request.setCredentials(credentials);
    CommandResponse response = tcpClient.send(request);
    if (response.isSuccess()) {
      tcpClient.setCredentials(credentials);
    }
    return toResult(response);
  }

  private CommandResponse send(CommandType type, CommandPayload payload) throws Exception {
    if (!type.isMutating()) {
      return tcpClient.send(new CommandRequest(type, payload));
    }
    String opKey = operationKey(type, payload);
    String requestId = idempotencyKeys.requestIdFor(opKey);
    CommandResponse response = tcpClient.send(new CommandRequest(type, payload, requestId));
    idempotencyKeys.resolve(opKey);
    return response;
  }

  private static String operationKey(CommandType type, CommandPayload payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(type.name().getBytes(StandardCharsets.UTF_8));
      if (payload != null) {
        digest.update(Serializer.serialize(payload));
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException | IOException e) {
      throw new IllegalStateException("Не удалось вычислить ключ операции идемпотентности", e);
    }
  }

  public static GatewayResult toResult(CommandResponse response) {
    return response.isSuccess()
        ? GatewayResult.success(response.getMessage())
        : GatewayResult.error(response.getMessage());
  }

  private void runScript(Path path, Deque<Path> stack, List<String> output) throws Exception {
    if (!Files.isRegularFile(path)) {
      throw new IOException("Файл скрипта не найден: " + path);
    }
    if (stack.size() >= MAX_SCRIPT_DEPTH) {
      throw new IOException("Ограничение рекурсии execute_script: максимум " + MAX_SCRIPT_DEPTH);
    }
    if (stack.contains(path)) {
      throw new IOException("Обнаружена рекурсия execute_script: " + path);
    }

    stack.push(path);
    try (Scanner scanner = new Scanner(path, StandardCharsets.UTF_8)) {
      while (scanner.hasNextLine()) {
        String rawCommand = scanner.nextLine();
        if (rawCommand == null || rawCommand.isBlank()) {
          continue;
        }
        output.add("> " + rawCommand.trim());
        CommandRequest request = parser.parse(rawCommand);
        if (request != null) {
          processScriptRequest(request, rawCommand, scanner, path.getParent(), stack, output);
        }
      }
    } finally {
      stack.pop();
    }
  }

  private void processScriptRequest(
      CommandRequest request,
      String rawCommand,
      Scanner scanner,
      Path baseDirectory,
      Deque<Path> stack,
      List<String> output)
      throws Exception {
    CommandType type = request.getCommandType();
    if (type == CommandType.EXECUTE_SCRIPT) {
      runScript(resolveScriptPath(rawCommand, baseDirectory), stack, output);
      return;
    }
    if (type == CommandType.EXIT) {
      output.add("exit пропущен в GUI");
      return;
    }
    if (!type.isServerTransmittable() && type != CommandType.UNKNOWN) {
      output.add("Команда не поддерживается в GUI: " + type.getWireName());
      return;
    }
    CommandRequest enriched = enrichScriptPayload(request, scanner, output);
    if (enriched != null) {
      appendResponse(tcpClient.send(enriched), output);
    }
  }

  private CommandRequest enrichScriptPayload(
      CommandRequest request, Scanner scanner, List<String> output) {
    CommandType type = request.getCommandType();
    if (type == CommandType.ADD || type == CommandType.ADD_IF_MIN) {
      SpaceMarine marine = scriptSpaceMarineReader.read(scanner, false);
      return new CommandRequest(type, new MarinePayload(marine));
    }
    if (type == CommandType.UPDATE) {
      IdPayload idPayload = CommandPayloads.findIdPayload(request).orElse(null);
      if (idPayload == null) {
        output.add("Использование: update <id>");
        return null;
      }
      SpaceMarine marine = scriptSpaceMarineReader.read(scanner, false);
      return new CommandRequest(type, new IdMarinePayload(idPayload.getId(), marine));
    }
    return request;
  }

  private static Path resolveScriptPath(String rawCommand, Path baseDirectory) throws IOException {
    String[] parts = rawCommand.trim().split("\\s+", 2);
    if (parts.length < 2 || parts[1].isBlank()) {
      throw new IOException("Использование: execute_script <file_name>");
    }
    Path rawPath = Path.of(parts[1].trim());
    if (rawPath.isAbsolute() || baseDirectory == null) {
      return rawPath.toAbsolutePath().normalize();
    }
    return baseDirectory.resolve(rawPath).toAbsolutePath().normalize();
  }

  private static void appendResponse(CommandResponse response, List<String> output) {
    if (response.getMessage() != null && !response.getMessage().isBlank()) {
      output.add(response.getMessage());
    }
    if (response.getData() != null && !response.getData().isEmpty()) {
      response.getData().forEach(item -> output.add(item.toString()));
    }
  }
}
