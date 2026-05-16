package dev.mikhalexandr.server.managers;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.request.payload.RawArgumentsPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.commands.CommandContract;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Хранит зарегистрированные команды и исполняет их по строковому вводу. */
public class CommandManager implements CommandExecutor {
  private static final int HISTORY_SIZE = 5;
  private final Map<String, CommandContract> commands = new LinkedHashMap<>();
  private final List<String> history = new ArrayList<>();

  /**
   * Регистрирует новую команду.
   *
   * @param command экземпляр команды
   */
  public void register(CommandContract command) {
    if (commands.containsKey(command.getName())) {
      throw new IllegalStateException(
          String.format("Команда '%s' уже зарегистрирована", command.getName()));
    }
    commands.put(command.getName(), command);
  }

  /**
   * Выполняет команду из запроса и возвращает ответ.
   *
   * @param request запрос на выполнение команды
   * @return результат выполнения
   */
  @Override
  public CommandResponse execute(CommandRequest request) {
    CommandResponse result;
    if (request == null || request.getCommandType() == null) {
      result = CommandResponse.error("Пустая команда");
    } else {
      CommandType commandType = request.getCommandType();
      if (commandType == CommandType.UNKNOWN) {
        result =
            CommandResponse.error(
                String.format(
                    "Неизвестная команда: %s. Введите help для отображения списка команд",
                    extractUnknownCommandName(request)));
      } else {
        String name = commandType.getWireName();
        CommandContract command = commands.get(name);
        if (command == null) {
          result =
              CommandResponse.error(
                  String.format(
                      "Неизвестная команда: %s. Введите help для отображения списка команд", name));
        } else {
          try {
            CommandResponse response = command.execute(request);
            addToHistory(name, command.getArgs());
            result = response;
          } catch (CommandExecutionException e) {
            result =
                CommandResponse.error(
                    String.format("Ошибка выполнения команды: %s", e.getMessage()));
          }
        }
      }
    }
    return result;
  }

  private static String extractUnknownCommandName(CommandRequest request) {
    if (request.getPayload() instanceof RawArgumentsPayload rawArgumentsPayload) {
      String[] arguments = rawArgumentsPayload.getArguments();
      if (arguments.length > 0) {
        return arguments[0];
      }
    }
    return "<unknown>";
  }

  /**
   * Добавляет название команды в историю с ограничением по размеру.
   *
   * @param name имя выполненной команды
   */
  public void addToHistory(String name, String args) {
    String info = name + " " + args;
    history.add(info);
    if (history.size() > HISTORY_SIZE) {
      history.removeFirst();
    }
  }

  /**
   * @return список недавно выполненных команд
   */
  public List<String> getHistory() {
    return Collections.unmodifiableList(history);
  }

  /**
   * @return карта зарегистрированных команд
   */
  public Map<String, CommandContract> getCommands() {
    return Collections.unmodifiableMap(commands);
  }
}
