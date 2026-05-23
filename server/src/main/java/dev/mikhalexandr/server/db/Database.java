package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.util.Env;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Доступа к PostgreSQL с простым пулом соединений */
public final class Database implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
  private static final int DEFAULT_POOL_SIZE = 8;
  private static final int VALIDATION_TIMEOUT_SECONDS = 3;
  private static final String ENV_POOL_SIZE = "DB_POOL_SIZE";
  private static final String DRIVER_CLASS = "org.postgresql.Driver";

  private final DatabaseConfig config;
  private final BlockingQueue<Connection> idle;
  private volatile boolean closed;

  /**
   * Открывает пул соединений
   *
   * @param config конфигурация подключения
   */
  public Database(DatabaseConfig config) {
    this.config = config;
    loadDriver();
    int poolSize = resolvePoolSize();
    this.idle = new ArrayBlockingQueue<>(poolSize);
    for (int i = 0; i < poolSize; i++) {
      idle.add(openConnection());
    }
    LOGGER.info("Пул соединений с БД открыт: {} (соединений: {})", config.describe(), poolSize);
  }

  private static void loadDriver() {
    try {
      Class.forName(DRIVER_CLASS);
    } catch (ClassNotFoundException e) {
      throw new DataAccessException("JDBC-драйвер PostgreSQL не найден в classpath", e);
    }
  }

  /**
   * Выполняет что-то в рамках одного соединения из пула
   *
   * @param callback действие над соединением
   * @param <T> тип результата
   * @return результат действия
   */
  public <T> T execute(ConnectionCallback<T> callback) {
    Connection connection = borrow();
    try {
      return callback.run(connection);
    } catch (SQLException e) {
      throw new DataAccessException("Ошибка выполнения запроса к БД: " + e.getMessage(), e);
    } finally {
      release(connection);
    }
  }

  @Override
  public void close() {
    closed = true;
    List<Connection> drained = new ArrayList<>();
    idle.drainTo(drained);
    drained.forEach(Database::closeQuietly);
    LOGGER.info("Пул соединений с БД закрыт");
  }

  private Connection borrow() {
    try {
      Connection connection = idle.take();
      if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
        return connection;
      }
      closeQuietly(connection);
      return openConnection();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DataAccessException("Ожидание соединения с БД прервано", e);
    } catch (SQLException e) {
      throw new DataAccessException("Соединение с БД недоступно: " + e.getMessage(), e);
    }
  }

  private void release(Connection connection) {
    if (closed || !idle.offer(connection)) {
      closeQuietly(connection);
    }
  }

  private Connection openConnection() {
    try {
      return DriverManager.getConnection(config.url(), config.user(), config.password());
    } catch (SQLException e) {
      throw new DataAccessException("Не удалось подключиться к БД: " + e.getMessage(), e);
    }
  }

  private static int resolvePoolSize() {
    String raw = Env.orDefault(ENV_POOL_SIZE, null);
    if (raw == null) {
      return DEFAULT_POOL_SIZE;
    }
    try {
      int parsed = Integer.parseInt(raw.trim());
      return parsed > 0 ? parsed : DEFAULT_POOL_SIZE;
    } catch (NumberFormatException e) {
      return DEFAULT_POOL_SIZE;
    }
  }

  private static void closeQuietly(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      LOGGER.debug("Не удалось закрыть соединение с БД", e);
    }
  }

  /** Действие над соединением с бдхой */
  @FunctionalInterface
  public interface ConnectionCallback<T> {
    /**
     * @param connection соединение из пула
     * @return результат действия
     * @throws SQLException если запрос завершился ошибкой
     */
    T run(Connection connection) throws SQLException;
  }
}
