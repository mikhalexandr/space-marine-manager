package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.managers.CollectionManager;

import java.util.stream.Collectors;

/**
 * Команда {@code show}: выводит все элементы коллекции.
 */
public class ShowCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public ShowCommand(CollectionManager collectionManager) {
        super("show", "вывести все элементы коллекции");
        this.collectionManager = collectionManager;
    }

    /**
     * Печатает все элементы коллекции в строковом представлении.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        if (collectionManager.size() == 0) {
            return "Коллекция пуста";
        }
        return collectionManager.getCollection().stream()
                .map(Object::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
