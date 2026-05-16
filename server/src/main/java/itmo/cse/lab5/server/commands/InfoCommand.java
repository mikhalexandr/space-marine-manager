package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code info}: выводит информацию о текущей коллекции.
 */
public class InfoCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public InfoCommand(CollectionManager collectionManager) {
        super("info", "вывести информацию о коллекции");
        this.collectionManager = collectionManager;
    }

    /**
     * Печатает тип коллекции, дату инициализации и количество элементов.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        return String.format("Тип коллекции: %s%nДата инициализации: %s%nКоличество элементов: %d",
                collectionManager.getType(), collectionManager.getInitializationDateFormatted(), collectionManager.size());
    }
}
