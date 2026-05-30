package dev.mikhalexandr.server.db;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.Chapter;
import dev.mikhalexandr.common.models.Coordinates;
import dev.mikhalexandr.common.models.MeleeWeapon;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.exceptions.DataAccessException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Реализация {@link SpaceMarineRepository} */
public final class JooqSpaceMarineRepository implements SpaceMarineRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(JooqSpaceMarineRepository.class);

  private static final Table<Record> SPACE_MARINES = table(name("space_marines"));
  private static final Field<Integer> ID = field(name("id"), SQLDataType.INTEGER);
  private static final Field<String> NAME = field(name("name"), SQLDataType.VARCHAR);
  private static final Field<Double> COORDINATE_X = field(name("coordinate_x"), SQLDataType.DOUBLE);
  private static final Field<Long> COORDINATE_Y = field(name("coordinate_y"), SQLDataType.BIGINT);
  private static final Field<Timestamp> CREATION_DATE =
      field(name("creation_date"), SQLDataType.TIMESTAMP);
  private static final Field<Float> HEALTH = field(name("health"), SQLDataType.REAL);
  private static final Field<Long> HEIGHT = field(name("height"), SQLDataType.BIGINT);
  private static final Field<String> CATEGORY = field(name("category"), SQLDataType.VARCHAR);
  private static final Field<String> MELEE_WEAPON =
      field(name("melee_weapon"), SQLDataType.VARCHAR);
  private static final Field<String> CHAPTER_NAME =
      field(name("chapter_name"), SQLDataType.VARCHAR);
  private static final Field<String> CHAPTER_PARENT_LEGION =
      field(name("chapter_parent_legion"), SQLDataType.VARCHAR);
  private static final Field<String> OWNER_LOGIN = field(name("owner_login"), SQLDataType.VARCHAR);

  private final Database database;

  public JooqSpaceMarineRepository(Database database) {
    this.database = database;
  }

  @Override
  public void insert(SpaceMarine marine, String ownerLogin) {
    database.execute(connection -> doInsert(connection, marine, ownerLogin));
  }

  @Override
  public boolean update(int id, SpaceMarine marine) {
    return database.execute(connection -> doUpdate(connection, id, marine));
  }

  @Override
  public boolean deleteByIdAndOwner(int id, String ownerLogin) {
    return database.execute(
        connection ->
            DSL.using(connection, SQLDialect.POSTGRES)
                    .deleteFrom(SPACE_MARINES)
                    .where(ID.eq(id).and(OWNER_LOGIN.eq(ownerLogin)))
                    .execute()
                > 0);
  }

  @Override
  public int deleteByOwner(String ownerLogin) {
    return database.execute(
        connection ->
            DSL.using(connection, SQLDialect.POSTGRES)
                .deleteFrom(SPACE_MARINES)
                .where(OWNER_LOGIN.eq(ownerLogin))
                .execute());
  }

  @Override
  public Optional<SpaceMarine> findById(int id) {
    return database.execute(
        connection -> {
          Record record =
              DSL.using(connection, SQLDialect.POSTGRES)
                  .selectFrom(SPACE_MARINES)
                  .where(ID.eq(id))
                  .fetchOne();
          return record == null ? Optional.empty() : Optional.ofNullable(tryMapRow(record));
        });
  }

  @Override
  public List<SpaceMarine> findAll() {
    return database.execute(JooqSpaceMarineRepository::doFindAll);
  }

  private static Void doInsert(Connection connection, SpaceMarine marine, String ownerLogin) {
    Record generated =
        DSL.using(connection, SQLDialect.POSTGRES)
            .insertInto(SPACE_MARINES)
            .set(NAME, marine.getName())
            .set(COORDINATE_X, marine.getCoordinates().getX())
            .set(COORDINATE_Y, marine.getCoordinates().getY())
            .set(HEALTH, marine.getHealth())
            .set(HEIGHT, marine.getHeight())
            .set(CATEGORY, marine.getCategory().name())
            .set(MELEE_WEAPON, weaponName(marine))
            .set(CHAPTER_NAME, chapterName(marine))
            .set(CHAPTER_PARENT_LEGION, chapterLegion(marine))
            .set(OWNER_LOGIN, ownerLogin)
            .returning(ID, CREATION_DATE)
            .fetchOne();
    applyGeneratedFields(generated, marine);
    marine.setOwner(ownerLogin);
    return null;
  }

  private static boolean doUpdate(Connection connection, int id, SpaceMarine marine) {
    return DSL.using(connection, SQLDialect.POSTGRES)
            .update(SPACE_MARINES)
            .set(NAME, marine.getName())
            .set(COORDINATE_X, marine.getCoordinates().getX())
            .set(COORDINATE_Y, marine.getCoordinates().getY())
            .set(HEALTH, marine.getHealth())
            .set(HEIGHT, marine.getHeight())
            .set(CATEGORY, marine.getCategory().name())
            .set(MELEE_WEAPON, weaponName(marine))
            .set(CHAPTER_NAME, chapterName(marine))
            .set(CHAPTER_PARENT_LEGION, chapterLegion(marine))
            .where(ID.eq(id))
            .execute()
        > 0;
  }

  private static List<SpaceMarine> doFindAll(Connection connection) {
    DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);
    List<SpaceMarine> result = new ArrayList<>();
    for (Record record : dsl.selectFrom(SPACE_MARINES).orderBy(ID.asc()).fetch()) {
      SpaceMarine marine = tryMapRow(record);
      if (marine != null) {
        result.add(marine);
      }
    }
    return result;
  }

  private static void applyGeneratedFields(Record generated, SpaceMarine marine) {
    if (generated == null) {
      throw new DataAccessException("INSERT не вернул сгенерированные поля id/creation_date", null);
    }
    marine.setId(generated.get(ID));
    marine.setCreationDate(toDate(generated.get(CREATION_DATE)));
  }

  private static SpaceMarine tryMapRow(Record record) {
    Integer id = record.get(ID);
    try {
      return mapRow(record, id);
    } catch (RuntimeException e) {
      LOGGER.warn("Пропущена некорректная строка space_marines (id={}): {}", id, e.getMessage());
      return null;
    }
  }

  private static SpaceMarine mapRow(Record record, Integer id) {
    Coordinates coordinates = new Coordinates(record.get(COORDINATE_X), record.get(COORDINATE_Y));
    SpaceMarine marine =
        new SpaceMarine(
            record.get(NAME),
            coordinates,
            record.get(HEALTH),
            record.get(HEIGHT),
            AstartesCategory.valueOf(record.get(CATEGORY)),
            mapMeleeWeapon(record.get(MELEE_WEAPON)),
            mapChapter(record));
    marine.setId(id);
    marine.setCreationDate(toDate(record.get(CREATION_DATE)));
    marine.setOwner(record.get(OWNER_LOGIN));
    return marine;
  }

  private static Chapter mapChapter(Record record) {
    String name = record.get(CHAPTER_NAME);
    if (name == null) {
      return null;
    }
    return new Chapter(name, record.get(CHAPTER_PARENT_LEGION));
  }

  private static MeleeWeapon mapMeleeWeapon(String value) {
    return value == null ? null : MeleeWeapon.valueOf(value);
  }

  private static Date toDate(Timestamp value) {
    return new Date(value.getTime());
  }

  private static String weaponName(SpaceMarine marine) {
    return marine.getMeleeWeapon() == null ? null : marine.getMeleeWeapon().name();
  }

  private static String chapterName(SpaceMarine marine) {
    return marine.getChapter() == null ? null : marine.getChapter().getName();
  }

  private static String chapterLegion(SpaceMarine marine) {
    return marine.getChapter() == null ? null : marine.getChapter().getParentLegion();
  }
}
