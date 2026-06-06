package dev.mikhalexandr.server.db;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class JooqUserRepository implements UserRepository {
  private static final Table<Record> USERS = table(name("users"));
  private static final Field<String> LOGIN = field(name("login"), SQLDataType.VARCHAR);
  private static final Field<String> PASSWORD_HASH =
      field(name("password_hash"), SQLDataType.VARCHAR);

  private final Database database;

  public JooqUserRepository(Database database) {
    this.database = database;
  }

  @Override
  public boolean exists(String login) {
    return database.execute(
        connection -> {
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          return dsl.fetchExists(dsl.selectOne().from(USERS).where(LOGIN.eq(login)));
        });
  }

  @Override
  public void create(String login, String passwordHash) {
    database.execute(
        connection -> {
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          dsl.insertInto(USERS).set(LOGIN, login).set(PASSWORD_HASH, passwordHash).execute();
          return null;
        });
  }

  @Override
  public Optional<String> findPasswordHash(String login) {
    return database.execute(
        connection -> {
          DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
          return dsl.select(PASSWORD_HASH)
              .from(USERS)
              .where(LOGIN.eq(login))
              .fetchOptional(PASSWORD_HASH);
        });
  }
}
