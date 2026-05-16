package itmo.cse.lab5.server.app;

import itmo.cse.lab5.server.commands.factory.CommandFactory;
import itmo.cse.lab5.server.commands.factory.DefaultCommandFactory;
import itmo.cse.lab5.server.exceptions.FileReadException;
import itmo.cse.lab5.server.io.CommandOutput;
import itmo.cse.lab5.server.io.CommandReader;
import itmo.cse.lab5.server.io.CommandRequestParser;
import itmo.cse.lab5.server.io.InputHandler;
import itmo.cse.lab5.server.managers.CollectionManager;
import itmo.cse.lab5.server.managers.CommandExecutor;
import itmo.cse.lab5.server.managers.CommandManager;
import itmo.cse.lab5.server.managers.FileManager;
import itmo.cse.lab5.server.managers.proxy.LoggingCommandExecutorProxy;
import itmo.cse.lab5.server.managers.proxy.ValidatingCommandExecutorProxy;

import java.util.Scanner;

/**
 * Выполняет инициализацию зависимостей серверного приложения.
 */
public class ApplicationBootstrap {
    /**
     * Собирает зависимости, загружает коллекцию и регистрирует команды.
     *
     * @param filePath путь к JSON-файлу коллекции
     * @param scanner источник пользовательского ввода
     * @return контекст выполнения командного цикла
     */
    public ApplicationContext bootstrap(String filePath, Scanner scanner) {
        CollectionManager collectionManager = new CollectionManager();
        FileManager fileManager = new FileManager(filePath);
        CommandManager commandManager = new CommandManager();
        CommandExecutor commandExecutor = new ValidatingCommandExecutorProxy(
                new LoggingCommandExecutorProxy(commandManager)
        );
        InputHandler inputHandler = new InputHandler(scanner);
        CommandReader commandReader = new CommandReader(scanner);
        CommandRequestParser commandRequestParser = new CommandRequestParser();
        CommandOutput commandOutput = new CommandOutput();

        loadCollection(collectionManager, fileManager, commandOutput);
        registerCommands(commandManager, commandExecutor, collectionManager, inputHandler, commandRequestParser, fileManager);

        return new ApplicationContext(commandExecutor, commandReader, commandRequestParser, commandOutput);
    }

    private void loadCollection(CollectionManager collectionManager,
                                FileManager fileManager,
                                CommandOutput commandOutput) {
        try {
            collectionManager.setCollection(fileManager.load());
            commandOutput.printMessage(String.format("Загружено элементов: %d", collectionManager.size()));
        } catch (FileReadException e) {
            commandOutput.printError(String.format("Ошибка загрузки: %s", e.getMessage()));
        }
    }

    private void registerCommands(CommandManager commandManager,
                                  CommandExecutor commandExecutor,
                                  CollectionManager collectionManager,
                                  InputHandler inputHandler,
                                  CommandRequestParser commandRequestParser,
                                  FileManager fileManager) {
        CommandFactory commandFactory = new DefaultCommandFactory(
                commandManager,
                commandExecutor,
                collectionManager,
                inputHandler,
                commandRequestParser,
                fileManager
        );
        commandManager.registerAll(commandFactory.createCommands());
    }
}
