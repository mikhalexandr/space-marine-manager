package dev.mikhalexandr.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
  private static final String EXISTS_SQL = "SELECT 1 FROM users WHERE login = ?";
  private static final String INSERT_SQL = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
  private static final String FIND_HASH_SQL = "SELECT password_hash FROM users WHERE login = ?";

  private final Database database;

  public JdbcUserRepository(Database database) {
    this.database = database;
  }

  @Override
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

  @Override
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

  @Override
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
