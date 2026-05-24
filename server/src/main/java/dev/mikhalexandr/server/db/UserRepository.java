package dev.mikhalexandr.server.db;

import java.util.Optional;

public interface UserRepository {
  boolean exists(String login);

  void create(String login, String passwordHash);

  Optional<String> findPasswordHash(String login);
}
