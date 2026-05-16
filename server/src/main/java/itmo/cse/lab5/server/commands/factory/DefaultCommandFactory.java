package itmo.cse.lab5.server.commands.factory;

import itmo.cse.lab5.server.commands.*;
import itmo.cse.lab5.server.io.CommandRequestParser;
import itmo.cse.lab5.server.io.InputHandler;
import itmo.cse.lab5.server.managers.CollectionManager;
import itmo.cse.lab5.server.managers.CommandExecutor;
import itmo.cse.lab5.server.managers.CommandManager;
import itmo.cse.lab5.server.managers.FileManager;

import java.util.Map;

/**
 * Стандартная фабрика команд сервера.
 */
public class DefaultCommandFactory implements CommandFactory {
    private final CommandManager commandManager;
    private final CommandExecutor commandExecutor;
    private final CollectionManager collectionManager;
    private final InputHandler inputHandler;
    private final CommandRequestParser commandRequestParser;
    private final FileManager fileManager;

    /**
     * @param commandManager менеджер команд
      * @param commandExecutor исполнитель команд
     * @param collectionManager менеджер коллекции
     * @param inputHandler обработчик ввода
     * @param commandRequestParser парсер командного ввода
     * @param fileManager файловый менеджер
     */
    public DefaultCommandFactory(CommandManager commandManager,
                                 CommandExecutor commandExecutor,
                                 CollectionManager collectionManager,
                                 InputHandler inputHandler,
                                 CommandRequestParser commandRequestParser,
                                 FileManager fileManager) {
        this.commandManager = commandManager;
        this.commandExecutor = commandExecutor;
        this.collectionManager = collectionManager;
        this.inputHandler = inputHandler;
        this.commandRequestParser = commandRequestParser;
        this.fileManager = fileManager;
    }

    @Override
    public Map<String, CommandContract> createCommands() {
        CommandRegistry registry = new CommandRegistry();
        registerSystemCommands(registry);
        registerCollectionCommands(registry);
        registerScriptAndPersistenceCommands(registry);
        return registry.toMap();
    }

    private void registerSystemCommands(CommandRegistry registry) {
        registry.register(new HelpCommand(commandManager));
        registry.register(new HistoryCommand(commandManager));
        registry.register(new ExitCommand());
    }

    private void registerCollectionCommands(CommandRegistry registry) {
        registry.register(new InfoCommand(collectionManager));
        registry.register(new ShowCommand(collectionManager));
        registry.register(new AddCommand(collectionManager, inputHandler));
        registry.register(new UpdateCommand(collectionManager, inputHandler));
        registry.register(new RemoveByIdCommand(collectionManager));
        registry.register(new ClearCommand(collectionManager));
        registry.register(new HeadCommand(collectionManager));
        registry.register(new AddIfMinCommand(collectionManager, inputHandler));
        registry.register(new SumOfHealthCommand(collectionManager));
        registry.register(new MaxByChapterCommand(collectionManager));
        registry.register(new CountByCategoryCommand(collectionManager));
    }

    private void registerScriptAndPersistenceCommands(CommandRegistry registry) {
        registry.register(new SaveCommand(collectionManager, fileManager));
        registry.register(new ExecuteScriptCommand(commandExecutor, inputHandler, commandRequestParser));
    }
}
