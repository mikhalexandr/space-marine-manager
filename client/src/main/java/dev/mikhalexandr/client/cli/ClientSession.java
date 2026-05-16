package dev.mikhalexandr.client.cli;

import dev.mikhalexandr.client.commands.CommandRequestParser;
import dev.mikhalexandr.client.io.InputHandler;
import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
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
import java.util.Scanner;

/** Интерактивная сессия клиента: цикл ввода и выполнение команд. */
public final class ClientSession {
  private static final int MAX_SCRIPT_DEPTH = 3;
  private static final boolean IS_INTERACTIVE_CONSOLE = System.console() != null;

  private final CommandRequestParser parser;
  private final InputHandler marineInputReader;
  private final TcpClient tcpClient;

  public ClientSession(
      CommandRequestParser parser, InputHandler marineInputReader, TcpClient tcpClient) {
    this.parser = parser;
    this.marineInputReader = marineInputReader;
    this.tcpClient = tcpClient;
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
    boolean continueLoop = true;
    if (request != null) {
      CommandType commandType = request.getCommandType();
      if (commandType == CommandType.EXIT) {
        continueLoop = false;
      } else if (commandType != CommandType.UNKNOWN && !commandType.isServerTransmittable()) {
        printResponseLine("Эта команда недоступна в клиенте");
      } else if (commandType == CommandType.EXECUTE_SCRIPT) {
        pushScriptInput(rawCommand, inputs);
      } else if (commandType != CommandType.UPDATE || ensureUpdateTargetExists(request)) {
        processRequestWithEntityPayload(request, currentInput);
      }
    }
    // паттерн стратегия
    return continueLoop;
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
      if (!(request.getPayload() instanceof IdPayload idPayload)) {
        printResponseLine("Использование: update <id>");
        return null;
      }

      SpaceMarine marine = marineInputReader.read(scanner, showPrompts);
      return new CommandRequest(CommandType.UPDATE, new IdMarinePayload(idPayload.getId(), marine));
    }

    return request;
  }

  private boolean ensureUpdateTargetExists(CommandRequest request) {
    if (!(request.getPayload() instanceof IdPayload)) {
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
