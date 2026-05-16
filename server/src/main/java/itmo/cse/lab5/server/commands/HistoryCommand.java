package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.managers.CommandManager;

import java.util.stream.Collectors;

/**
 * Команда {@code history}: выводит историю выполненных команд.
 */
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
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        var history = commandManager.getHistory();
        if (history.isEmpty()) {
            return "История команд пуста";
        }
        return "Последние команды:" + System.lineSeparator()
                + history.stream().map(command -> "  " + command).collect(Collectors.joining(System.lineSeparator()));
    }
}
