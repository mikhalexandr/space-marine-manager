package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.models.SpaceMarine;
import itmo.cse.lab5.server.io.InputHandler;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code add}: добавляет новый элемент в коллекцию.
 */
public class AddCommand extends Command {
    private final CollectionManager collectionManager;
    private final InputHandler inputHandler;

    /**
     * @param collectionManager менеджер коллекции
     * @param inputHandler обработчик интерактивного ввода
     */
    public AddCommand(CollectionManager collectionManager, InputHandler inputHandler) {
        super("add", "{element}", "добавить новый элемент в коллекцию");
        this.collectionManager = collectionManager;
        this.inputHandler = inputHandler;
    }

    /**
     * Считывает элемент и добавляет его в коллекцию.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     * @throws CommandExecutionException если не удалось дочитать поля элемента
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        SpaceMarine spaceMarine = inputHandler.readSpaceMarine();
        collectionManager.add(spaceMarine);
        return "Элемент успешно добавлен";
    }
}
