package dev.mikhalexandr.server.db;

import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Создаёт схему бд при старте сервера */
public final class SchemaInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaInitializer.class);

  private static final String CREATE_USERS_TABLE =
      """
      CREATE TABLE IF NOT EXISTS users (
          id            BIGSERIAL PRIMARY KEY,
          login         VARCHAR(64) NOT NULL UNIQUE,
          password_hash VARCHAR(32) NOT NULL
      )""";

  private static final String CREATE_MARINE_SEQUENCE =
      "CREATE SEQUENCE IF NOT EXISTS space_marine_id_seq AS INTEGER START WITH 1 INCREMENT BY 1";

  private static final String CREATE_MARINES_TABLE =
      """
      CREATE TABLE IF NOT EXISTS space_marines (
          id                    INTEGER PRIMARY KEY DEFAULT nextval('space_marine_id_seq'),
          name                  VARCHAR(255) NOT NULL,
          coordinate_x          DOUBLE PRECISION NOT NULL,
          coordinate_y          BIGINT NOT NULL,
          creation_date         TIMESTAMP NOT NULL DEFAULT now(),
          health                REAL NOT NULL,
          height                BIGINT NOT NULL,
          category              VARCHAR(32) NOT NULL,
          melee_weapon          VARCHAR(32),
          chapter_name          VARCHAR(255),
          chapter_parent_legion VARCHAR(255),
          owner_login           VARCHAR(64) NOT NULL REFERENCES users(login) ON DELETE CASCADE
      )""";

  private final Database database;

  /**
   * @param database корка доступа к бд
   */
  public SchemaInitializer(Database database) {
    this.database = database;
  }

  /** Создаёт недостающие объекты схемы */
  public void initialize() {
    database.execute(
        connection -> {
          try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_USERS_TABLE);
            statement.execute(CREATE_MARINE_SEQUENCE);
            statement.execute(CREATE_MARINES_TABLE);
          }
          return null;
        });
    LOGGER.info("Схема БД четенькая: users, space_marine_id_seq, space_marines");
  }
}
