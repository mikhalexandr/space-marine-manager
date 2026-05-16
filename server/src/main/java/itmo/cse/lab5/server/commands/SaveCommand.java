package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.server.exceptions.FileWriteException;
import itmo.cse.lab5.server.managers.CollectionManager;
import itmo.cse.lab5.server.managers.FileManager;

/**
 * Команда {@code save}: сохраняет коллекцию в файл.
 */
public class SaveCommand extends Command {
    private final CollectionManager collectionManager;
    private final FileManager fileManager;

    /**
     * @param collectionManager менеджер коллекции
     * @param fileManager менеджер файловых операций
     */
    public SaveCommand(CollectionManager collectionManager, FileManager fileManager) {
        super("save", "сохранит коллекцию в файл");
        this.collectionManager = collectionManager;
        this.fileManager = fileManager;
    }

    /**
     * Сохраняет текущую коллекцию в файл.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     * @throws CommandExecutionException если сохранение не удалось
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        try {
            fileManager.save(collectionManager.getCollection());
            return "Коллекция сохранена";
        } catch (FileWriteException e) {
            throw new CommandExecutionException("Ошибка сохранения: " + e.getMessage());
        }
    }
}
