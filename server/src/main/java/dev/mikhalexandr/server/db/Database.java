package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.util.Env;
import dev.mikhalexandr.server.exceptions.DataAccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Database implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
  private static final int DEFAULT_POOL_SIZE = 8;
  private static final int VALIDATION_TIMEOUT_SECONDS = 3;
  private static final String ENV_POOL_SIZE = "DB_POOL_SIZE";
  private static final String DRIVER_CLASS = "org.postgresql.Driver";
  private static final int LOCK_TIMEOUT_MILLIS = 3000;
  private static final int STATEMENT_TIMEOUT_MILLIS = 10000;

  private final DatabaseConfig config;
  private final BlockingQueue<Connection> idle;
  private final ThreadLocal<Connection> activeTransaction = new ThreadLocal<>();
  private volatile boolean closed;

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

  public <T> T execute(ConnectionCallback<T> callback) {
    Connection bound = activeTransaction.get();
    if (bound != null) {
      try {
        return callback.run(bound);
      } catch (SQLException e) {
        throw new DataAccessException("Ошибка выполнения запроса к БД: " + e.getMessage(), e);
      }
    }
    Connection connection = borrow();
    try {
      return callback.run(connection);
    } catch (SQLException e) {
      throw new DataAccessException("Ошибка выполнения запроса к БД: " + e.getMessage(), e);
    } finally {
      release(connection);
    }
  }

  public <T> T inTransaction(ConnectionCallback<T> callback) {
    if (activeTransaction.get() != null) {
      return execute(callback);
    }
    Connection connection = borrow();
    activeTransaction.set(connection);
    boolean committed = false;
    try {
      connection.setAutoCommit(false);
      applyTransactionTimeouts(connection);
      T result = callback.run(connection);
      connection.commit();
      committed = true;
      return result;
    } catch (SQLException e) {
      throw new DataAccessException("Ошибка транзакции БД: " + e.getMessage(), e);
    } finally {
      if (!committed) {
        rollbackQuietly(connection);
      }
      activeTransaction.remove();
      restoreAutoCommit(connection);
      release(connection);
    }
  }

  private static void applyTransactionTimeouts(Connection connection) throws SQLException {
    try (java.sql.Statement statement = connection.createStatement()) {
      statement.execute("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT_MILLIS + "'");
      statement.execute("SET LOCAL statement_timeout = '" + STATEMENT_TIMEOUT_MILLIS + "'");
    }
  }

  private static void rollbackQuietly(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException e) {
      LOGGER.warn("Не удалось откатить транзакцию: {}", e.getMessage());
    }
  }

  private static void restoreAutoCommit(Connection connection) {
    try {
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      LOGGER.debug("Не удалось вернуть autoCommit=true", e);
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

  @FunctionalInterface
  public interface ConnectionCallback<T> {
    T run(Connection connection) throws SQLException;
  }
}
