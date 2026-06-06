package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandManager;
import java.util.stream.Collectors;

public class HistoryCommand extends Command {
  private final CommandManager commandManager;

  public HistoryCommand(CommandManager commandManager) {
    super("history", "вывести последние 5 команд");
    this.commandManager = commandManager;
  }

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
