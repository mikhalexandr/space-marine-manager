package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.models.SpaceMarine;
import itmo.cse.lab5.server.io.InputHandler;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code add_if_min}: добавляет элемент, если он меньше минимального.
 */
public class AddIfMinCommand extends Command {
    private final CollectionManager collectionManager;
    private final InputHandler inputHandler;

    /**
     * @param collectionManager менеджер коллекции
     * @param inputHandler обработчик интерактивного ввода
     */
    public AddIfMinCommand(CollectionManager collectionManager, InputHandler inputHandler) {
        super("add_if_min", "{element}", "добавить элемент, если он меньше минимального");
        this.collectionManager = collectionManager;
        this.inputHandler = inputHandler;
    }

    /**
     * Считывает элемент и пытается добавить его по условию {@code add_if_min}.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     * @throws CommandExecutionException если не удалось дочитать поля элемента
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        SpaceMarine spaceMarine = inputHandler.readSpaceMarine();
        if (collectionManager.addIfMin(spaceMarine)) {
            return "Элемент добавлен";
        } else {
            return "Элемент НЕ добавлен, так как он НЕ меньше минимального";
        }
    }
}
