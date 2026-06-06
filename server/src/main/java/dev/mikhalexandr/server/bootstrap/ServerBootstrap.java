package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.common.util.Env;
import dev.mikhalexandr.server.auth.AuthService;
import dev.mikhalexandr.server.db.Database;
import dev.mikhalexandr.server.db.DatabaseConfig;
import dev.mikhalexandr.server.db.IdempotencyStore;
import dev.mikhalexandr.server.db.JooqIdempotencyStore;
import dev.mikhalexandr.server.db.JooqSpaceMarineRepository;
import dev.mikhalexandr.server.db.JooqUserRepository;
import dev.mikhalexandr.server.db.SchemaInitializer;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.managers.CommandManager;
import dev.mikhalexandr.server.managers.proxy.AuthenticatingInterceptor;
import dev.mikhalexandr.server.managers.proxy.CommandExecutorProxyFactory;
import dev.mikhalexandr.server.managers.proxy.IdempotencyInterceptor;
import dev.mikhalexandr.server.managers.proxy.LoggingInterceptor;
import dev.mikhalexandr.server.managers.proxy.RateLimitingInterceptor;
import dev.mikhalexandr.server.managers.proxy.ValidatingInterceptor;
import dev.mikhalexandr.server.network.TcpServer;
import dev.mikhalexandr.server.ratelimit.RedisRateLimiter;
import dev.mikhalexandr.server.security.ServerIdentity;
import dev.mikhalexandr.server.security.VaultPkiClient;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

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
  private static final String ENV_IDEMPOTENCY_RETENTION_HOURS = "IDEMPOTENCY_RETENTION_HOURS";
  private static final String ENV_IDEMPOTENCY_CLEANUP_MINUTES = "IDEMPOTENCY_CLEANUP_MINUTES";
  private static final long DEFAULT_IDEMPOTENCY_RETENTION_HOURS = 24;
  private static final long DEFAULT_IDEMPOTENCY_CLEANUP_PERIOD_MINUTES = 30;
  private static final int RATE_LIMIT_MAX_REQUESTS = 100;
  private static final long RATE_LIMIT_WINDOW_MILLIS = 10_000L;
  private static final String ENV_REDIS_HOST = "REDIS_HOST";
  private static final String ENV_REDIS_PORT = "REDIS_PORT";
  private static final String ENV_REDIS_USERNAME = "REDIS_USERNAME";
  private static final String ENV_REDIS_PASSWORD = "REDIS_PASSWORD";
  private static final String DEFAULT_REDIS_HOST = "localhost";
  private static final String DEFAULT_REDIS_PORT = "6379";

  private final CommandRegistryInitializer commandRegistryInitializer =
      new CommandRegistryInitializer();

  public void run(int port) {
    LOGGER.info("Инициализация серверных зависимостей");
    DatabaseConfig databaseConfig = DatabaseConfig.fromEnv();
    LOGGER.info("Подключение к PostgreSQL: {}", databaseConfig.describe());
    Database database = new Database(databaseConfig);
    new SchemaInitializer(database).initialize();

    SpaceMarineRepository marineRepository = new JooqSpaceMarineRepository(database);
    CollectionManager collectionManager = new CollectionManager();
    collectionManager.loadAll(marineRepository.findAll());
    LOGGER.info("Коллекция загружена из БД: {} элементов", collectionManager.size());

    AuthService authService = new AuthService(new JooqUserRepository(database));
    CommandManager commandManager = new CommandManager();
    commandRegistryInitializer.register(commandManager, collectionManager, marineRepository);

    ServerIdentity identity = loadServerIdentity();
    LOGGER.info(
        "Серверная личность загружена: subject={}",
        identity.certificate().getSubjectX500Principal());

    IdempotencyStore idempotencyStore = new JooqIdempotencyStore(database);
    IdempotencyInterceptor idempotencyInterceptor =
        new IdempotencyInterceptor(database, idempotencyStore);
    ScheduledExecutorService idempotencyCleanup = startIdempotencyCleanup(idempotencyInterceptor);

    RedisRateLimiter rateLimiter = buildRateLimiter();
    RateLimitingInterceptor rateLimitingInterceptor = new RateLimitingInterceptor(rateLimiter);
    CommandExecutor commandExecutor =
        buildCommandExecutor(
            commandManager, rateLimitingInterceptor, idempotencyInterceptor, authService);
    TcpServer tcpServer = new TcpServer(port, commandExecutor, identity);
    collectionManager.setEventPublisher(tcpServer.eventPublisher());
    registerShutdownHook(tcpServer, database, idempotencyCleanup, rateLimiter);
    tcpServer.run();
  }

  private static ScheduledExecutorService startIdempotencyCleanup(
      IdempotencyInterceptor interceptor) {
    long retentionHours =
        Env.longOrDefault(ENV_IDEMPOTENCY_RETENTION_HOURS, DEFAULT_IDEMPOTENCY_RETENTION_HOURS);
    long cleanupMinutes =
        Env.longOrDefault(
            ENV_IDEMPOTENCY_CLEANUP_MINUTES, DEFAULT_IDEMPOTENCY_CLEANUP_PERIOD_MINUTES);
    return interceptor.startCleanup(
        Duration.ofHours(retentionHours), Duration.ofMinutes(cleanupMinutes));
  }

  private static CommandExecutor buildCommandExecutor(
      CommandManager commandManager,
      RateLimitingInterceptor rateLimiter,
      IdempotencyInterceptor idempotencyInterceptor,
      AuthService authService) {
    return CommandExecutorProxyFactory.create(
        commandManager,
        new ValidatingInterceptor(),
        rateLimiter,
        new AuthenticatingInterceptor(authService),
        idempotencyInterceptor,
        new LoggingInterceptor());
  }

  private static RedisRateLimiter buildRateLimiter() {
    String redisHost = Env.orDefault(ENV_REDIS_HOST, DEFAULT_REDIS_HOST);
    int redisPort = Integer.parseInt(Env.orDefault(ENV_REDIS_PORT, DEFAULT_REDIS_PORT));
    String redisUsername = Env.orDefault(ENV_REDIS_USERNAME, null);
    String redisPassword = Env.orDefault(ENV_REDIS_PASSWORD, null);
    if (redisUsername == null || redisPassword == null) {
      throw new IllegalStateException("Не заданы REDIS_USERNAME и REDIS_PASSWORD");
    }
    LOGGER.info("Rate limiter: Redis {}:{} (user={})", redisHost, redisPort, redisUsername);
    return new RedisRateLimiter(
        new JedisPool(redisHost, redisPort, redisUsername, redisPassword),
        RATE_LIMIT_MAX_REQUESTS,
        RATE_LIMIT_WINDOW_MILLIS);
  }

  private static void registerShutdownHook(
      TcpServer tcpServer,
      Database database,
      ScheduledExecutorService idempotencyCleanup,
      RedisRateLimiter rateLimiter) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(() -> shutdown(tcpServer, database, idempotencyCleanup, rateLimiter)));
  }

  private static void shutdown(
      TcpServer tcpServer,
      Database database,
      ScheduledExecutorService idempotencyCleanup,
      RedisRateLimiter rateLimiter) {
    tcpServer.stop();
    idempotencyCleanup.shutdownNow();
    rateLimiter.close();
    database.close();
  }

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
