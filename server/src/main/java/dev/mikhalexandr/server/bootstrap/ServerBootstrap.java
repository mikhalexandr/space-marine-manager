package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.common.util.Env;
import dev.mikhalexandr.server.auth.AuthService;
import dev.mikhalexandr.server.db.Database;
import dev.mikhalexandr.server.db.DatabaseConfig;
import dev.mikhalexandr.server.db.JdbcSpaceMarineRepository;
import dev.mikhalexandr.server.db.JdbcUserRepository;
import dev.mikhalexandr.server.db.SchemaInitializer;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.managers.CommandManager;
import dev.mikhalexandr.server.managers.proxy.AuthenticatingInterceptor;
import dev.mikhalexandr.server.managers.proxy.CommandExecutorProxyFactory;
import dev.mikhalexandr.server.managers.proxy.IdempotencyInterceptor;
import dev.mikhalexandr.server.managers.proxy.LoggingInterceptor;
import dev.mikhalexandr.server.managers.proxy.ValidatingInterceptor;
import dev.mikhalexandr.server.network.TcpServer;
import dev.mikhalexandr.server.security.ServerIdentity;
import dev.mikhalexandr.server.security.VaultPkiClient;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Собирает зависимости, поднимает БД и коллекцию и запускает все, что может */
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

  /**
   * Запускает сервак
   *
   * @param port TCP-порт прослушивания
   */
  public void run(int port) {
    LOGGER.info("Инициализация серверных зависимостей");
    DatabaseConfig databaseConfig = DatabaseConfig.fromEnv();
    LOGGER.info("Подключение к PostgreSQL: {}", databaseConfig.describe());
    Database database = new Database(databaseConfig);
    new SchemaInitializer(database).initialize();

    SpaceMarineRepository marineRepository = new JdbcSpaceMarineRepository(database);
    CollectionManager collectionManager = new CollectionManager();
    collectionManager.loadAll(marineRepository.findAll());
    LOGGER.info("Коллекция загружена из БД: {} элементов", collectionManager.size());

    AuthService authService = new AuthService(new JdbcUserRepository(database));
    CommandManager commandManager = new CommandManager();
    commandRegistryInitializer.register(commandManager, collectionManager, marineRepository);

    ServerIdentity identity = loadServerIdentity();
    LOGGER.info(
        "Серверная личность загружена: subject={}",
        identity.certificate().getSubjectX500Principal());

    CommandExecutor commandExecutor =
        CommandExecutorProxyFactory.create(
            commandManager,
            new IdempotencyInterceptor(),
            new LoggingInterceptor(),
            new ValidatingInterceptor(),
            new AuthenticatingInterceptor(authService));
    TcpServer tcpServer = new TcpServer(port, commandExecutor, identity);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(tcpServer, database)));
    tcpServer.run();
  }

  private static void shutdown(TcpServer tcpServer, Database database) {
    tcpServer.stop();
    database.close();
  }

  /** Идёт во Vault и крафтит серверный сертификат через CSR */
  private ServerIdentity loadServerIdentity() {
    String vaultUrl = Env.orDefault(ENV_VAULT_URL, null);
    if (vaultUrl == null) {
      throw new IllegalStateException("VAULT_URL не задан");
    }
    String role = Env.orDefault(ENV_VAULT_PKI_ROLE, DEFAULT_VAULT_PKI_ROLE);
    String commonName = Env.orDefault(ENV_VAULT_COMMON_NAME, DEFAULT_VAULT_COMMON_NAME);
    String roleId = Env.orDefault(ENV_VAULT_ROLE_ID, null);
    String secretId = Env.orDefault(ENV_VAULT_SECRET_ID, null);
    String token = Env.orDefault(ENV_VAULT_TOKEN, null);

    try {
      VaultPkiClient client = chooseAuth(vaultUrl, role, roleId, secretId, token);
      LOGGER.info(
          "Источник серверной личности: Vault {} (pki_role={}, CN={})", vaultUrl, role, commonName);
      return client.provisionIdentity(commonName);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Не удалось получить серт от Vault (" + vaultUrl + "): " + e.getMessage(), e);
    }
  }

  private static VaultPkiClient chooseAuth(
      String url, String pkiRole, String roleId, String secretId, String token) throws IOException {
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
