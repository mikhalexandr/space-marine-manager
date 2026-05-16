package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code clear}: очищает коллекцию.
 */
public class ClearCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public ClearCommand(CollectionManager collectionManager) {
        super("clear", "очистить коллекцию");
        this.collectionManager = collectionManager;
    }

    /**
     * Очищает коллекцию.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        collectionManager.clear();
        return "Коллекция очищена.";
    }
}
