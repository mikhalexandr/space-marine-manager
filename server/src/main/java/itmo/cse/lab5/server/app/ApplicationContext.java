package itmo.cse.lab5.server.app;

import itmo.cse.lab5.server.io.CommandOutput;
import itmo.cse.lab5.server.io.CommandReader;
import itmo.cse.lab5.server.io.CommandRequestParser;
import itmo.cse.lab5.server.managers.CommandExecutor;

/**
 * Контекст зависимостей для цикла обработки команд.
 */
public class ApplicationContext {
    private final CommandExecutor commandExecutor;
    private final CommandReader commandReader;
    private final CommandRequestParser commandRequestParser;
    private final CommandOutput commandOutput;

    /**
     * @param commandExecutor исполнитель команд
     * @param commandReader читатель строковых команд
     * @param commandRequestParser парсер строковых команд
     * @param commandOutput обработчик вывода
     */
    public ApplicationContext(CommandExecutor commandExecutor,
                              CommandReader commandReader,
                              CommandRequestParser commandRequestParser,
                              CommandOutput commandOutput) {
        this.commandExecutor = commandExecutor;
        this.commandReader = commandReader;
        this.commandRequestParser = commandRequestParser;
        this.commandOutput = commandOutput;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public CommandReader getCommandReader() {
        return commandReader;
    }

    public CommandRequestParser getCommandRequestParser() {
        return commandRequestParser;
    }

    public CommandOutput getCommandOutput() {
        return commandOutput;
    }
}
