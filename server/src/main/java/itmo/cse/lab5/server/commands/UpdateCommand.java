package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.models.SpaceMarine;
import itmo.cse.lab5.server.io.InputHandler;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code update}: обновляет элемент коллекции по id.
 */
public class UpdateCommand extends Command {
    private final CollectionManager collectionManager;
    private final InputHandler inputHandler;

    /**
     * @param collectionManager менеджер коллекции
     * @param inputHandler обработчик интерактивного ввода
     */
    public UpdateCommand(CollectionManager collectionManager, InputHandler inputHandler) {
        super("update", "<id> {element}", "обновить элемент коллекции по id");
        this.collectionManager = collectionManager;
        this.inputHandler = inputHandler;
    }

    /**
     * Обновляет элемент с переданным id новыми данными, считанными из консоли.
     *
     * @param args ожидается один аргумент: id
     * @return текст результата выполнения
     * @throws CommandExecutionException если id не передан, некорректен или элемент не найден
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        if (args.length == 0) {
            throw new CommandExecutionException("Использование: update <id>");
        }
        try {
            String[] idArgs = args[0].trim().split("\\s+");
            if (idArgs.length != 1) {
                throw new CommandExecutionException("Формат: update <id>, затем ввод полей с новой строки");
            }

            int id = Integer.parseInt(idArgs[0]);
            SpaceMarine oldSpaceMarine = collectionManager.getById(id);
            if (oldSpaceMarine == null) {
                throw new CommandExecutionException(
                        String.format("Элемент с id=%d не найден", id)
                );
            }

            SpaceMarine spaceMarine = inputHandler.readSpaceMarine(oldSpaceMarine);
            collectionManager.update(id, spaceMarine);
            return String.format("Элемент успешно обновлён%nНовое значение элемента:%n%s", collectionManager.getById(id));
        } catch (NumberFormatException e) {
            throw new CommandExecutionException("id должен быть числом");
        }
    }
}
