package itmo.cse.lab5.server.commands.factory;

import itmo.cse.lab5.server.commands.CommandContract;

import java.util.Map;

/**
 * Фабрика создания команд.
 */
public interface CommandFactory {
    /**
     * @return карта всех доступных команд
     */
    Map<String, CommandContract> createCommands();
}
