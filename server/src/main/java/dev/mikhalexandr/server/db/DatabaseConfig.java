package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.util.Env;

/**
 * Конфигурация подключения к постгре
 *
 */
public final class DatabaseConfig {
  private static final String PROFILE_HELIOS = "helios";
  private static final String PROFILE_DOCKER = "docker";
  private static final String DEFAULT_PROFILE = PROFILE_DOCKER;

  private static final String HELIOS_URL = "jdbc:postgresql://pg:5432/studs";
  private static final String DOCKER_URL = "jdbc:postgresql://localhost:5432/space_marine";
  private static final String DOCKER_USER = "smm";
  private static final String DOCKER_PASSWORD = "smm";

  private static final String ENV_PROFILE = "DB_PROFILE";
  private static final String ENV_URL = "DB_URL";
  private static final String ENV_USER = "DB_USER";
  private static final String ENV_PASSWORD = "DB_PASSWORD";
  private static final String ENV_SSH_USERNAME = "SSH_USERNAME";

  private final String profile;
  private final String url;
  private final String user;
  private final String password;

  private DatabaseConfig(String profile, String url, String user, String password) {
    this.profile = profile;
    this.url = url;
    this.user = user;
    this.password = password;
  }

  /**
   * Собирает конфигурацию из энва
   *
   * @return готовая конфигурация подключения
   */
  public static DatabaseConfig fromEnv() {
    String profile = Env.orDefault(ENV_PROFILE, DEFAULT_PROFILE).trim().toLowerCase();
    String url = Env.orDefault(ENV_URL, defaultUrl(profile));
    String user = Env.orDefault(ENV_USER, defaultUser(profile));
    String password = Env.orDefault(ENV_PASSWORD, defaultPassword(profile));
    validate(profile, url, user, password);
    return new DatabaseConfig(profile, url, user, password);
  }

  private static String defaultUrl(String profile) {
    return PROFILE_HELIOS.equals(profile) ? HELIOS_URL : DOCKER_URL;
  }

  private static String defaultUser(String profile) {
    if (PROFILE_HELIOS.equals(profile)) {
      return Env.orDefault(ENV_SSH_USERNAME, null);
    }
    return DOCKER_USER;
  }

  private static String defaultPassword(String profile) {
    return PROFILE_HELIOS.equals(profile) ? null : DOCKER_PASSWORD;
  }

  private static void validate(String profile, String url, String user, String password) {
    if (url == null || url.isBlank()) {
      throw new IllegalStateException("JDBC URL не задан (DB_URL, профиль " + profile + ")");
    }
    if (user == null || user.isBlank()) {
      throw new IllegalStateException(
          "Имя пользователя БД не задано: укажите DB_USER (профиль " + profile + ")");
    }
    if (password == null) {
      throw new IllegalStateException(
          "Пароль БД не задан: укажите DB_PASSWORD (профиль " + profile + ")");
    }
  }

  /**
   * @return JDBC URL
   */
  public String url() {
    return url;
  }

  /**
   * @return имя пользователя бдхи
   */
  public String user() {
    return user;
  }

  /**
   * @return пароль пользователя бдхи
   */
  public String password() {
    return password;
  }

  /**
   * @return краткое описание подключения без пароля, для логов четких
   */
  public String describe() {
    return String.format("profile=%s, url=%s, user=%s", profile, url, user);
  }
}
