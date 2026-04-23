package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandManager;
import java.util.stream.Collectors;

/** Команда {@code history}: выводит историю выполненных команд. */
public class HistoryCommand extends Command {
  private final CommandManager commandManager;

  /**
   * @param commandManager менеджер команд
   */
  public HistoryCommand(CommandManager commandManager) {
    super("history", "вывести последние 5 команд");
    this.commandManager = commandManager;
  }

  /**
   * Печатает список последних выполненных команд.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   */
  @Override
  public CommandResponse execute(CommandRequest request) {
    var history = commandManager.getHistory();
    if (history.isEmpty()) {
      return CommandResponse.success("История команд пуста");
    }
    return CommandResponse.success(
        "Последние команды:"
            + System.lineSeparator()
            + history.stream()
                .map(command -> " " + command)
                .collect(Collectors.joining(System.lineSeparator())));
  }
}
