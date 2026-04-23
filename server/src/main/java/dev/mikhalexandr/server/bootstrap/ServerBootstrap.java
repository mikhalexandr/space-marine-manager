package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.managers.CommandManager;
import dev.mikhalexandr.server.managers.FileManager;
import dev.mikhalexandr.server.managers.proxy.LoggingCommandExecutorProxy;
import dev.mikhalexandr.server.managers.proxy.ValidatingCommandExecutorProxy;
import dev.mikhalexandr.server.network.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Собирает зависимости, загружает коллекцию и запускает основной цикл команд. */
public class ServerBootstrap {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerBootstrap.class);
  private final CommandRegistryInitializer commandRegistryInitializer =
      new CommandRegistryInitializer();
  private final CollectionBootstrapLoader collectionLoader = new CollectionBootstrapLoader();

  /** Запускает серверное приложение. */
  public void run(String collectionFilePath, int port) {
    LOGGER.info("Инициализация серверных зависимостей");
    CollectionManager collectionManager = new CollectionManager();
    FileManager fileManager = new FileManager(collectionFilePath);
    CommandManager commandManager = new CommandManager();

    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> fileManager.save(collectionManager.getCollection())));

    LOGGER.info("Загрузка коллекции из файла: {}", collectionFilePath);
    collectionLoader.load(collectionManager, fileManager);
    LOGGER.info("Регистрация команд сервера");
    commandRegistryInitializer.register(commandManager, collectionManager);

    LOGGER.info("Переход в цикл обработки TCP-запросов");
    CommandExecutor commandExecutor =
        new LoggingCommandExecutorProxy(new ValidatingCommandExecutorProxy(commandManager));
    TcpServer tcpServer = new TcpServer(port, commandExecutor);
    tcpServer.run();
  }
}
