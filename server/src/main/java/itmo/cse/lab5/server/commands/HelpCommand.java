package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.managers.CommandManager;

/**
 * Команда {@code help}: выводит список доступных команд.
 */
public class HelpCommand extends Command {
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
     * @param args аргументы команды (не используются)
     */
    @Override
    public String execute(String[] args) {
        StringBuilder builder = new StringBuilder();
        commandManager.getCommands()
                .forEach((name, command)
                        -> builder.append(String.format("  %-30s → %s%n",
                        command.getArgs().isEmpty() ? name : name + " " + command.getArgs(), command.getDescription())));
        return builder.toString().stripTrailing();
    }
}
