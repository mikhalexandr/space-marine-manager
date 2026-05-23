package dev.mikhalexandr.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/** Доступ к таблице пользователей {@code users} */
public final class UserRepository {
  private static final String EXISTS_SQL = "SELECT 1 FROM users WHERE login = ?";
  private static final String INSERT_SQL = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
  private static final String FIND_HASH_SQL = "SELECT password_hash FROM users WHERE login = ?";

  private final Database database;

  /**
   * @param database корочка доступа к бдхе
   */
  public UserRepository(Database database) {
    this.database = database;
  }

  /**
   * @param login логин пользователя
   * @return true, если пользователь с таким логином уже существует
   */
  public boolean exists(String login) {
    return database.execute(
        connection -> {
          try (PreparedStatement statement = connection.prepareStatement(EXISTS_SQL)) {
            statement.setString(1, login);
            try (ResultSet resultSet = statement.executeQuery()) {
              return resultSet.next();
            }
          }
        });
  }

  /**
   * Создаёт нового пользователя.
   *
   * @param login логин пользователя
   * @param passwordHash MD2-хэш пароля в hex-представлении
   */
  public void create(String login, String passwordHash) {
    database.execute(
        connection -> {
          try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, login);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
          }
          return null;
        });
  }

  /**
   * @param login логин пользователя
   * @return сохранённый MD2-хэш пароля или пустое значение, если пользователя нету
   */
  public Optional<String> findPasswordHash(String login) {
    return database.execute(
        connection -> {
          try (PreparedStatement statement = connection.prepareStatement(FIND_HASH_SQL)) {
            statement.setString(1, login);
            try (ResultSet resultSet = statement.executeQuery()) {
              return resultSet.next()
                  ? Optional.of(resultSet.getString("password_hash"))
                  : Optional.empty();
            }
          }
        });
  }
}
