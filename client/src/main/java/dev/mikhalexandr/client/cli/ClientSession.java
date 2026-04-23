package dev.mikhalexandr.client.cli;

import dev.mikhalexandr.client.cli.strategy.CommandStrategy;
import dev.mikhalexandr.client.cli.strategy.ExecuteScriptCommandStrategy;
import dev.mikhalexandr.client.cli.strategy.ExitCommandStrategy;
import dev.mikhalexandr.client.cli.strategy.ServerCommandStrategy;
import dev.mikhalexandr.client.cli.strategy.StrategyActions;
import dev.mikhalexandr.client.cli.strategy.UnsupportedClientCommandStrategy;
import dev.mikhalexandr.client.cli.strategy.UpdateCommandStrategy;
import dev.mikhalexandr.client.commands.CommandRequestParser;
import dev.mikhalexandr.client.io.InputHandler;
import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.IdMarinePayload;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.request.payload.MarinePayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Scanner;

/** Интерактивная сессия клиента: цикл ввода и выполнение команд. */
public final class ClientSession {
  private static final int MAX_SCRIPT_DEPTH = 3;
  private static final boolean IS_INTERACTIVE_CONSOLE = System.console() != null;

  private final CommandRequestParser parser;
  private final InputHandler marineInputReader;
  private final TcpClient tcpClient;
  private final Map<CommandType, CommandStrategy> commandStrategies;
  private final CommandStrategy defaultCommandStrategy;
  private final CommandStrategy unsupportedClientCommandStrategy;

  public ClientSession(
      CommandRequestParser parser, InputHandler marineInputReader, TcpClient tcpClient) {
    this.parser = parser;
    this.marineInputReader = marineInputReader;
    this.tcpClient = tcpClient;
    this.defaultCommandStrategy = new ServerCommandStrategy();
    this.unsupportedClientCommandStrategy = new UnsupportedClientCommandStrategy();
    this.commandStrategies = createCommandStrategies();
  }

  /** Запускает цикл чтения команд из консоли/скриптов. */
  public void run() {
    try (Scanner scanner = new Scanner(System.in)) {
      boolean running = true;
      Deque<InputContext> inputs = new ArrayDeque<>();
      inputs.push(InputContext.console(scanner));

      while (running && !inputs.isEmpty()) {
        InputContext currentInput = inputs.peek();
        if (currentInput.isConsole() && IS_INTERACTIVE_CONSOLE) {
          printPrompt();
        }
        if (!currentInput.scanner().hasNextLine()) {
          closeInputContext(inputs.pop());
          continue;
        }
        String rawCommand = currentInput.scanner().nextLine();
        if (!currentInput.isConsole() && !rawCommand.isBlank()) {
          System.out.println("> " + rawCommand);
        }
        running = processInputLine(rawCommand, currentInput, inputs);
      }

      while (!inputs.isEmpty()) {
        closeInputContext(inputs.pop());
      }
    }
  }

  private boolean processInputLine(
      String rawCommand, InputContext currentInput, Deque<InputContext> inputs) {
    CommandRequest request = parser.parse(rawCommand);
    if (request == null) {
      return true;
    }

    StrategyActions strategyActions =
        new StrategyActions(
            () -> processRequestWithEntityPayload(request, currentInput),
            () -> pushScriptInput(rawCommand, inputs),
            () -> ensureUpdateTargetExists(request),
            ClientSession::printResponseLine);
    CommandStrategy strategy = resolveCommandStrategy(request.getCommandType());
    return strategy.handle(strategyActions);
  }

  private Map<CommandType, CommandStrategy> createCommandStrategies() {
    Map<CommandType, CommandStrategy> strategies = new EnumMap<>(CommandType.class);
    strategies.put(CommandType.EXIT, new ExitCommandStrategy());
    strategies.put(CommandType.EXECUTE_SCRIPT, new ExecuteScriptCommandStrategy());
    strategies.put(CommandType.UPDATE, new UpdateCommandStrategy());
    return strategies;
  }

  private CommandStrategy resolveCommandStrategy(CommandType commandType) {
    CommandStrategy strategy = commandStrategies.get(commandType);
    if (strategy != null) {
      return strategy;
    }

    if (commandType != CommandType.UNKNOWN && !commandType.isServerTransmittable()) {
      return unsupportedClientCommandStrategy;
    }

    return defaultCommandStrategy;
  }

  private void processRequestWithEntityPayload(CommandRequest request, InputContext currentInput) {
    try {
      CommandRequest enrichedRequest =
          enrichPayloadForEntityCommands(request, currentInput.scanner(), currentInput.isConsole());
      if (enrichedRequest != null) {
        sendAndPrintResponse(enrichedRequest);
      }
    } catch (InputHandler.InputReadException e) {
      printResponseLine(e.getMessage());
    }
  }

  private static void printPrompt() {
    System.out.print("> ");
  }

  private void pushScriptInput(String rawCommand, Deque<InputContext> inputs) {
    String fileName = extractScriptFileName(rawCommand);
    if (fileName == null || fileName.isBlank()) {
      printResponseLine("Использование: execute_script <file_name>");
      return;
    }

    if (scriptDepth(inputs) >= MAX_SCRIPT_DEPTH) {
      printResponseLine(
          String.format(
              "Ограничение рекурсии execute_script: максимум %d вложенных скриптов",
              MAX_SCRIPT_DEPTH));
      return;
    }

    try {
      Path scriptPath = resolveScriptPath(fileName);
      if (containsPath(inputs, scriptPath)) {
        printResponseLine(
            "Обнаружена рекурсия execute_script: "
                + scriptPath
                + " (лимит вложенности: "
                + MAX_SCRIPT_DEPTH
                + ")");
        return;
      }

      Scanner scriptScanner = new Scanner(scriptPath, StandardCharsets.UTF_8);
      inputs.push(InputContext.script(scriptScanner, scriptPath));
    } catch (IOException e) {
      printResponseLine("Не удалось прочитать файл скрипта: " + fileName);
    }
  }

  private static String extractScriptFileName(String rawCommand) {
    if (rawCommand == null) {
      return null;
    }
    String[] parts = rawCommand.trim().split("\\s+", 2);
    if (parts.length < 2) {
      return null;
    }
    String fileName = parts[1].trim();
    return fileName.isEmpty() ? null : fileName;
  }

  private static Path resolveScriptPath(String fileName) {
    Path rawPath = Path.of(fileName.trim());
    return rawPath.toAbsolutePath().normalize();
  }

  private static int scriptDepth(Deque<InputContext> inputs) {
    int depth = 0;
    for (InputContext input : inputs) {
      if (!input.isConsole()) {
        depth++;
      }
    }
    return depth;
  }

  private static boolean containsPath(Deque<InputContext> inputs, Path path) {
    for (InputContext input : inputs) {
      if (path.equals(input.path())) {
        return true;
      }
    }
    return false;
  }

  private static void closeInputContext(InputContext input) {
    if (!input.isConsole()) {
      input.scanner().close();
    }
  }

  private CommandRequest enrichPayloadForEntityCommands(
      CommandRequest request, Scanner scanner, boolean showPrompts) {
    if (request.getCommandType() == CommandType.ADD
        || request.getCommandType() == CommandType.ADD_IF_MIN) {
      SpaceMarine marine = marineInputReader.read(scanner, showPrompts);
      return new CommandRequest(request.getCommandType(), new MarinePayload(marine));
    }

    if (request.getCommandType() == CommandType.UPDATE) {
      IdPayload idPayload = CommandPayloads.findIdPayload(request).orElse(null);
      if (idPayload == null) {
        printResponseLine("Использование: update <id>");
        return null;
      }

      SpaceMarine marine = marineInputReader.read(scanner, showPrompts);
      return new CommandRequest(CommandType.UPDATE, new IdMarinePayload(idPayload.getId(), marine));
    }

    return request;
  }

  private boolean ensureUpdateTargetExists(CommandRequest request) {
    if (CommandPayloads.findIdPayload(request).isEmpty()) {
      printResponseLine("Использование: update <id>");
      return false;
    }

    try {
      CommandResponse response = tcpClient.send(request);
      if (!response.isSuccess()) {
        if (response.getMessage() != null && !response.getMessage().isBlank()) {
          printResponseLine(response.getMessage());
        }
        return false;
      }
      return true;
    } catch (IOException e) {
      printResponseLine("Сервер временно недоступен. Повторите попытку позже.");
      return false;
    }
  }

  private void sendAndPrintResponse(CommandRequest request) {
    try {
      CommandResponse response = tcpClient.send(request);
      if (response.getMessage() != null && !response.getMessage().isBlank()) {
        printResponseLine(response.getMessage());
      }

      if (response.getData() != null && !response.getData().isEmpty()) {
        response.getData().forEach(item -> printResponseLine(item.toString()));
      }
    } catch (IOException e) {
      printResponseLine("Сервер временно недоступен. Повторите попытку позже.");
    }
  }

  private static void printResponseLine(String msg) {
    if (msg == null) {
      return;
    }
    for (String line : msg.split("\\r?\\n")) {
      System.out.println("| " + line);
    }
  }

  private static final class InputContext {
    private final Scanner scanner;
    private final Path path;
    private final boolean console;

    private InputContext(Scanner scanner, Path path, boolean console) {
      this.scanner = scanner;
      this.path = path;
      this.console = console;
    }

    private static InputContext console(Scanner scanner) {
      return new InputContext(scanner, null, true);
    }

    private static InputContext script(Scanner scanner, Path path) {
      return new InputContext(scanner, path, false);
    }

    private Scanner scanner() {
      return scanner;
    }

    private Path path() {
      return path;
    }

    private boolean isConsole() {
      return console;
    }
  }
}
