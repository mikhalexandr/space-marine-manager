package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code sum_of_health}: выводит сумму поля health по коллекции.
 */
public class SumOfHealthCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public SumOfHealthCommand(CollectionManager collectionManager) {
        super("sum_of_health", "вывести сумму здоровья всех элементов");
        this.collectionManager = collectionManager;
    }

    /**
     * Печатает сумму здоровья всех элементов.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        return String.format("Сумма здоровья: %f", collectionManager.sumOfHealth());
    }
}
