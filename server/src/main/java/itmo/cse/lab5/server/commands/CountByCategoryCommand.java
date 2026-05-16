package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.models.AstartesCategory;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code count_by_category}: выводит количество элементов заданной категории.
 */
public class CountByCategoryCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public CountByCategoryCommand(CollectionManager collectionManager) {
        super("count_by_category", "<category>", "вывести количество элементов с заданной категорией");
        this.collectionManager = collectionManager;
    }

    /**
     * Подсчитывает элементы с указанной категорией.
     *
     * @param args ожидается один аргумент: категория
     * @return текст результата выполнения
     * @throws CommandExecutionException если категория не передана или некорректна
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        if (args.length == 0) {
            throw new CommandExecutionException("Использование: count_by_category <category>");
        }
        try {
            AstartesCategory category = AstartesCategory.valueOf(args[0].toUpperCase());
            long count = collectionManager.countByCategory(category);
            return String.format("Количество элементов с категорией %s:%d", category, count);
        } catch (IllegalArgumentException e) {
            throw new CommandExecutionException("Неизвестная категория: " + args[0]);
        }
    }
}
