package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.common.models.SpaceMarine;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code head}: выводит первый элемент коллекции.
 */
public class HeadCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public HeadCommand(CollectionManager collectionManager) {
        super("head", "вывести первый элемент коллекции");
        this.collectionManager = collectionManager;
    }

    /**
     * Печатает первый элемент коллекции или сообщение о пустой коллекции.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        SpaceMarine head = collectionManager.head();
        if (head == null) {
            return "Коллекция пуста";
        } else {
            return head.toString();
        }
    }
}
