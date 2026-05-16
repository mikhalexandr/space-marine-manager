package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.common.models.SpaceMarine;
import itmo.cse.lab5.server.managers.CollectionManager;

/**
 * Команда {@code max_by_chapter}: выводит элемент с максимальным chapter.
 */
public class MaxByChapterCommand extends Command {
    private final CollectionManager collectionManager;

    /**
     * @param collectionManager менеджер коллекции
     */
    public MaxByChapterCommand(CollectionManager collectionManager) {
        super("max_by_chapter", "вывести с максимальным значением chapter");
        this.collectionManager = collectionManager;
    }

    /**
     * Выводит элемент с максимальным значением поля chapter.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        SpaceMarine max = collectionManager.maxByChapter();
        if (max == null) {
            return "Нет элементов с chapter";
        } else {
            return max.toString();
        }
    }
}
