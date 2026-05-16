package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code remove_by_id}: удаляет элемент коллекции по id.
 */
public class RemoveByIdCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public RemoveByIdCommand(CollectionManager collectionManager) {
        super("remove_by_id", "<id>", "удалить элемент по id");
        this.collectionManager = collectionManager;
    }

    /**
     * Удаляет элемент по идентификатору.
     *
     * @param args ожидается один аргумент: id
     * @return текст результата выполнения
     * @throws CommandExecutionException если id не передан или имеет неверный формат
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        if (args.length == 0) {
            throw new CommandExecutionException("Использование: remove_by_id <id>");
        }
        try {
            int id = Integer.parseInt(args[0]);
            if (collectionManager.removeById(id)) {
                return "Элемент удалён";
            } else {
                return String.format("Элемент с id=%d не найден", id);
            }
        } catch (NumberFormatException e) {
            throw new CommandExecutionException("id должен быть числом");
        }
    }
}
