package itmo.cse.lab5.server.commands.factory;

import itmo.cse.lab5.server.commands.CommandContract;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Регистрирует команды с проверкой уникальности имен.
 */
public class CommandRegistry {
    private final Map<String, CommandContract> commands = new LinkedHashMap<>();

    /**
     * Регистрирует команду.
     *
     * @param command команда
     */
    public void register(CommandContract command) {
        String commandName = command.getName();
        if (commands.containsKey(commandName)) {
            throw new IllegalStateException(String.format("Команда '%s' уже зарегистрирована", commandName));
        }
        commands.put(commandName, command);
    }

    /**
     * @return карта зарегистрированных команд
     */
    public Map<String, CommandContract> toMap() {
        return commands;
    }
}
