package dev.mikhalexandr.server.db;

import static org.jooq.impl.DSL.field;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Создаёт схему БД при старте сервера */
public final class SchemaInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaInitializer.class);

  private static final int LOGIN_LENGTH = 64;
  private static final int PASSWORD_HASH_LENGTH = 32;
  private static final int NAME_LENGTH = 255;
  private static final int CATEGORY_LENGTH = 32;
  private static final int KEY_LENGTH = 64;
  private static final int STATUS_LENGTH = 16;

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
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          createUsers(dsl);
          createMarines(dsl);
          createIdempotencyKeys(dsl);
          return null;
        });
    LOGGER.info("Схема БД четенькая");
  }

  private static void createUsers(DSLContext dsl) {
    dsl.createTableIfNotExists("users")
        .column("id", SQLDataType.BIGINT.identity(true))
        .column("login", SQLDataType.VARCHAR(LOGIN_LENGTH).nullable(false))
        .column("password_hash", SQLDataType.VARCHAR(PASSWORD_HASH_LENGTH).nullable(false))
        .constraints(
            DSL.constraint("pk_users").primaryKey("id"),
            DSL.constraint("uq_users_login").unique("login"))
        .execute();
  }

  private static void createMarines(DSLContext dsl) {
    dsl.createSequenceIfNotExists("space_marine_id_seq").execute();
    dsl.createTableIfNotExists("space_marines")
        .column(
            "id",
            SQLDataType.INTEGER
                .nullable(false)
                .defaultValue(field("nextval('space_marine_id_seq')", SQLDataType.INTEGER)))
        .column("name", SQLDataType.VARCHAR(NAME_LENGTH).nullable(false))
        .column("coordinate_x", SQLDataType.DOUBLE.nullable(false))
        .column("coordinate_y", SQLDataType.BIGINT.nullable(false))
        .column(
            "creation_date",
            SQLDataType.TIMESTAMP
                .nullable(false)
                .defaultValue(field("now()", SQLDataType.TIMESTAMP)))
        .column("health", SQLDataType.REAL.nullable(false))
        .column("height", SQLDataType.BIGINT.nullable(false))
        .column("category", SQLDataType.VARCHAR(CATEGORY_LENGTH).nullable(false))
        .column("melee_weapon", SQLDataType.VARCHAR(CATEGORY_LENGTH))
        .column("chapter_name", SQLDataType.VARCHAR(NAME_LENGTH))
        .column("chapter_parent_legion", SQLDataType.VARCHAR(NAME_LENGTH))
        .column("owner_login", SQLDataType.VARCHAR(LOGIN_LENGTH).nullable(false))
        .constraints(
            DSL.constraint("pk_space_marines").primaryKey("id"),
            DSL.constraint("fk_space_marines_owner")
                .foreignKey("owner_login")
                .references("users", "login")
                .onDeleteCascade())
        .execute();
  }

  private static void createIdempotencyKeys(DSLContext dsl) {
    dsl.createTableIfNotExists("idempotency_keys")
        .column("user_id", SQLDataType.VARCHAR(KEY_LENGTH).nullable(false))
        .column("request_id", SQLDataType.VARCHAR(KEY_LENGTH).nullable(false))
        .column("request_hash", SQLDataType.VARCHAR(KEY_LENGTH).nullable(false))
        .column("status", SQLDataType.VARCHAR(STATUS_LENGTH).nullable(false))
        .column("response", SQLDataType.BLOB)
        .column(
            "created_at",
            SQLDataType.LOCALDATETIME
                .nullable(false)
                .defaultValue(field("now()", SQLDataType.LOCALDATETIME)))
        .column("completed_at", SQLDataType.LOCALDATETIME)
        .constraints(DSL.constraint("pk_idempotency_keys").primaryKey("user_id", "request_id"))
        .execute();
    dsl.createIndexIfNotExists("idx_idempotency_created_at")
        .on("idempotency_keys", "created_at")
        .execute();
  }
}
