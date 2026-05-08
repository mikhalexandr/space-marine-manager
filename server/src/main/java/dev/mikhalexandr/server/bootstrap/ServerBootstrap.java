package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.common.util.Env;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.managers.CommandManager;
import dev.mikhalexandr.server.managers.FileManager;
import dev.mikhalexandr.server.managers.proxy.LoggingCommandExecutorProxy;
import dev.mikhalexandr.server.managers.proxy.ValidatingCommandExecutorProxy;
import dev.mikhalexandr.server.network.TcpServer;
import dev.mikhalexandr.server.security.ServerIdentity;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Собирает зависимости, загружает коллекцию и запускает основной цикл команд. */
public class ServerBootstrap {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerBootstrap.class);
  private static final String ENV_KEYSTORE_PATH = "KEYSTORE_PATH";
  private static final String ENV_KEYSTORE_PASSWORD = "KEYSTORE_PASSWORD";
  private static final String ENV_KEYSTORE_ALIAS = "KEYSTORE_ALIAS";
  private static final String DEFAULT_KEYSTORE_PATH = "certs/server.p12";
  private static final String DEFAULT_KEYSTORE_PASSWORD = "chuiz";
  private static final String DEFAULT_KEYSTORE_ALIAS = "server";

  private final CommandRegistryInitializer commandRegistryInitializer =
      new CommandRegistryInitializer();
  private final CollectionBootstrapLoader collectionLoader = new CollectionBootstrapLoader();

  /** Запускает серверное приложение. */
  public void run(String collectionFilePath, int port) {
    LOGGER.info("Инициализация серверных зависимостей");
    CollectionManager collectionManager = new CollectionManager();
    FileManager fileManager = new FileManager(collectionFilePath);
    CommandManager commandManager = new CommandManager();

    LOGGER.info("Загрузка коллекции из файла: {}", collectionFilePath);
    collectionLoader.load(collectionManager, fileManager);
    LOGGER.info("Регистрация команд сервера");
    commandRegistryInitializer.register(commandManager, collectionManager);

    ServerIdentity identity = loadServerIdentity();
    LOGGER.info(
        "Серверная личность загружена: subject={}, issuer={}",
        identity.certificate().getSubjectX500Principal(),
        identity.certificate().getIssuerX500Principal());

    LOGGER.info("Переход в цикл обработки TCP-запросов");
    CommandExecutor commandExecutor =
        new LoggingCommandExecutorProxy(new ValidatingCommandExecutorProxy(commandManager));
    TcpServer tcpServer = new TcpServer(port, commandExecutor, identity);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  tcpServer.stop();
                  fileManager.save(collectionManager.getCollection());
                }));
    tcpServer.run();
  }

  private ServerIdentity loadServerIdentity() {
    String keystorePath = Env.orDefault(ENV_KEYSTORE_PATH, DEFAULT_KEYSTORE_PATH);
    String keystorePassword = Env.orDefault(ENV_KEYSTORE_PASSWORD, DEFAULT_KEYSTORE_PASSWORD);
    String keystoreAlias = Env.orDefault(ENV_KEYSTORE_ALIAS, DEFAULT_KEYSTORE_ALIAS);
    try {
      return ServerIdentity.loadFromPkcs12(
          Path.of(keystorePath), keystorePassword.toCharArray(), keystoreAlias);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Не удалось загрузить " + keystorePath + " — запусти `task certs:gen`", e);
    }
  }
}
