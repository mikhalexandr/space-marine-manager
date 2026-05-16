package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;

/**
 * Контракт серверной команды.
 */
public interface CommandContract {
    /**
     * @return имя команды
     */
    String getName();

    /**
     * @return строка с описанием аргументов команды
     */
    String getArgs();

    /**
     * @return описание команды
     */
    String getDescription();

    /**
     * Выполняет команду.
     *
     * @param args аргументы команды
     * @return текст результата выполнения
     * @throws CommandExecutionException если выполнить команду не удалось
     */
    String execute(String[] args) throws CommandExecutionException;

    /**
     * @return true, если после выполнения команды сервер должен завершиться
     */
    boolean shouldExit();
}
