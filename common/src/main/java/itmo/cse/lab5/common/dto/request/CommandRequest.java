package itmo.cse.lab5.common.dto.request;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Запрос на выполнение команды.
 */
public final class CommandRequest implements Serializable {
    private final String commandName;
    private final String[] commandArguments;

    /**
     * @param commandName имя команды
     * @param commandArguments аргументы команды
     */
    public CommandRequest(String commandName, String[] commandArguments) {
        this.commandName = commandName;
        this.commandArguments = commandArguments == null ? new String[]{} : Arrays.copyOf(commandArguments,
                commandArguments.length);
    }

    /**
     * @return имя команды
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * @return копия массива аргументов
     */
    public String[] getCommandArguments() {
        return Arrays.copyOf(commandArguments, commandArguments.length);
    }
}
