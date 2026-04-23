package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandManager;

/** Команда {@code help}: выводит список доступных команд. */
public class HelpCommand extends Command {
  private static final String CLIENT_EXIT_HELP = "exit";
  private static final String CLIENT_EXIT_DESCRIPTION = "завершить клиентское приложение";
  private final CommandManager commandManager;

  /**
   * @param commandManager менеджер команд
   */
  public HelpCommand(CommandManager commandManager) {
    super("help", "вывести справку по доступным командам");
    this.commandManager = commandManager;
  }

  /**
   * Выводит справку по зарегистрированным командам.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   */
  @Override
  public CommandResponse execute(CommandRequest request) {
    StringBuilder builder = new StringBuilder();
    commandManager
        .getCommands()
        .forEach(
            (name, command) ->
                builder.append(
                    String.format(
                        "%-30s → %s%n",
                        command.getArgs().isEmpty() ? name : name + " " + command.getArgs(),
                        command.getDescription())));
    builder.append(String.format("%-30s → %s", CLIENT_EXIT_HELP, CLIENT_EXIT_DESCRIPTION));
    return CommandResponse.success(builder.toString().stripTrailing());
  }
}
