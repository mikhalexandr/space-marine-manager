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
import dev.mikhalexandr.server.security.VaultPkiClient;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Собирает зависимости, загружает коллекцию и запускает основной цикл команд. */
public class ServerBootstrap {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerBootstrap.class);

  private static final String ENV_VAULT_URL = "VAULT_URL";
  private static final String ENV_VAULT_TOKEN = "VAULT_TOKEN";
  private static final String ENV_VAULT_ROLE_ID = "VAULT_ROLE_ID";
  private static final String ENV_VAULT_SECRET_ID = "VAULT_SECRET_ID";
  private static final String ENV_VAULT_PKI_ROLE = "VAULT_PKI_ROLE";
  private static final String ENV_VAULT_COMMON_NAME = "VAULT_COMMON_NAME";
  private static final String DEFAULT_VAULT_PKI_ROLE = "server-role";
  private static final String DEFAULT_VAULT_COMMON_NAME = "localhost";

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

  /**
   * Идёт во Vault и провижионит серт через CSR. Vault - единственный источник серверной
   * идентичности; никаких файлов на диске.
   */
  private ServerIdentity loadServerIdentity() {
    String vaultUrl = Env.orDefault(ENV_VAULT_URL, null);
    if (vaultUrl == null) {
      throw new IllegalStateException(
          "VAULT_URL не задан");
    }
    String role = Env.orDefault(ENV_VAULT_PKI_ROLE, DEFAULT_VAULT_PKI_ROLE);
    String commonName = Env.orDefault(ENV_VAULT_COMMON_NAME, DEFAULT_VAULT_COMMON_NAME);
    String roleId = Env.orDefault(ENV_VAULT_ROLE_ID, null);
    String secretId = Env.orDefault(ENV_VAULT_SECRET_ID, null);
    String token = Env.orDefault(ENV_VAULT_TOKEN, null);

    try {
      VaultPkiClient client = chooseAuth(vaultUrl, role, roleId, secretId, token);
      LOGGER.info(
          "Источник серверной личности: Vault {} (pki_role={}, CN={})",
          vaultUrl, role, commonName);
      return client.provisionIdentity(commonName);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Не удалось получить серт от Vault (" + vaultUrl + "): " + e.getMessage(), e);
    }
  }

  private static VaultPkiClient chooseAuth(
      String url, String pkiRole, String roleId, String secretId, String token)
      throws IOException {
    if (roleId != null && secretId != null) {
      LOGGER.info("Vault auth: AppRole (role_id={})", roleId);
      return VaultPkiClient.withAppRole(url, roleId, secretId, pkiRole);
    }
    if (token != null) {
      return VaultPkiClient.withToken(url, token, pkiRole);
    }
    throw new IllegalStateException(
        "VAULT_URL задан, но не переданы ни VAULT_ROLE_ID+VAULT_SECRET_ID, ни VAULT_TOKEN");
  }
}
