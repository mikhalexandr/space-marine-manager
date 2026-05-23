package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.Chapter;
import dev.mikhalexandr.common.models.Coordinates;
import dev.mikhalexandr.common.models.MeleeWeapon;
import dev.mikhalexandr.common.models.SpaceMarine;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Доступ к таблице коллекции {@code space_marines} */
public final class SpaceMarineRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpaceMarineRepository.class);

  private static final String INSERT_SQL =
      """
      INSERT INTO space_marines
          (name, coordinate_x, coordinate_y, health, height, category,
           melee_weapon, chapter_name, chapter_parent_legion, owner_login)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      RETURNING id, creation_date""";

  private static final String UPDATE_SQL =
      """
      UPDATE space_marines SET
          name = ?, coordinate_x = ?, coordinate_y = ?, health = ?, height = ?,
          category = ?, melee_weapon = ?, chapter_name = ?, chapter_parent_legion = ?
      WHERE id = ?""";

  private static final String DELETE_BY_ID_SQL = "DELETE FROM space_marines WHERE id = ?";
  private static final String DELETE_BY_OWNER_SQL =
      "DELETE FROM space_marines WHERE owner_login = ?";
  private static final String SELECT_ALL_SQL = "SELECT * FROM space_marines ORDER BY id";

  private final Database database;

  public SpaceMarineRepository(Database database) {
    this.database = database;
  }

  /**
   * Вставляет объект
   *
   * @param marine добавляемый объект
   * @param ownerLogin логин пользователя который владелец
   */
  public void insert(SpaceMarine marine, String ownerLogin) {
    database.execute(connection -> doInsert(connection, marine, ownerLogin));
  }

  /**
   * Обновляет объект коллекции по id
   *
   * @param id идентификатор объекта
   * @param marine новое значение полей
   * @return true, если строка была обновлена
   */
  public boolean update(int id, SpaceMarine marine) {
    return database.execute(connection -> doUpdate(connection, id, marine));
  }

  /**
   * Удаляет объект по идентификатору
   *
   * @param id идентификатор объекта
   * @return true, если строка была удалена
   */
  public boolean deleteById(int id) {
    return database.execute(connection -> doDeleteById(connection, id));
  }

  /**
   * Удаляет все объекты указанного владельца
   *
   * @param ownerLogin логин пользователя-владельца
   * @return количество удалённых строк
   */
  public int deleteByOwner(String ownerLogin) {
    return database.execute(connection -> doDeleteByOwner(connection, ownerLogin));
  }

  /**
   * @return все объекты коллекции, отсортированные по id (некорректные строки пропускаются)
   */
  public List<SpaceMarine> findAll() {
    return database.execute(SpaceMarineRepository::doFindAll);
  }

  private static Void doInsert(Connection connection, SpaceMarine marine, String ownerLogin)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bindMarineColumns(new StatementBinder(statement), marine).string(ownerLogin);
      applyGeneratedFields(statement, marine);
    }
    marine.setOwner(ownerLogin);
    return null;
  }

  private static void applyGeneratedFields(PreparedStatement statement, SpaceMarine marine)
      throws SQLException {
    try (ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        throw new SQLException("INSERT не вернул сгенерированные поля id/creation_date");
      }
      marine.setId(resultSet.getInt("id"));
      marine.setCreationDate(new Date(resultSet.getTimestamp("creation_date").getTime()));
    }
  }

  private static boolean doUpdate(Connection connection, int id, SpaceMarine marine)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
      bindMarineColumns(new StatementBinder(statement), marine).intValue(id);
      return statement.executeUpdate() > 0;
    }
  }

  private static boolean doDeleteById(Connection connection, int id) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID_SQL)) {
      statement.setInt(1, id);
      return statement.executeUpdate() > 0;
    }
  }

  private static int doDeleteByOwner(Connection connection, String ownerLogin) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_OWNER_SQL)) {
      statement.setString(1, ownerLogin);
      return statement.executeUpdate();
    }
  }

  private static List<SpaceMarine> doFindAll(Connection connection) throws SQLException {
    List<SpaceMarine> result = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        SpaceMarine marine = tryMapRow(resultSet);
        if (marine != null) {
          result.add(marine);
        }
      }
    }
    return result;
  }

  private static StatementBinder bindMarineColumns(StatementBinder binder, SpaceMarine marine)
      throws SQLException {
    return binder
        .string(marine.getName())
        .doubleValue(marine.getCoordinates().getX())
        .longValue(marine.getCoordinates().getY())
        .floatValue(marine.getHealth())
        .longValue(marine.getHeight())
        .string(marine.getCategory().name())
        .nullableString(weaponName(marine))
        .nullableString(chapterName(marine))
        .nullableString(chapterLegion(marine));
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

  private static SpaceMarine tryMapRow(ResultSet resultSet) throws SQLException {
    int id = resultSet.getInt("id");
    try {
      return mapRow(resultSet, id);
    } catch (RuntimeException e) {
      LOGGER.warn("Пропущена некорректная строка space_marines (id={}): {}", id, e.getMessage());
      return null;
    }
  }

  private static SpaceMarine mapRow(ResultSet resultSet, int id) throws SQLException {
    Coordinates coordinates =
        new Coordinates(resultSet.getDouble("coordinate_x"), resultSet.getLong("coordinate_y"));
    SpaceMarine marine =
        new SpaceMarine(
            resultSet.getString("name"),
            coordinates,
            resultSet.getFloat("health"),
            resultSet.getLong("height"),
            AstartesCategory.valueOf(resultSet.getString("category")),
            mapMeleeWeapon(resultSet.getString("melee_weapon")),
            mapChapter(resultSet));
    marine.setId(id);
    marine.setCreationDate(new Date(resultSet.getTimestamp("creation_date").getTime()));
    marine.setOwner(resultSet.getString("owner_login"));
    return marine;
  }

  private static Chapter mapChapter(ResultSet resultSet) throws SQLException {
    String name = resultSet.getString("chapter_name");
    if (name == null) {
      return null;
    }
    return new Chapter(name, resultSet.getString("chapter_parent_legion"));
  }

  private static MeleeWeapon mapMeleeWeapon(String value) {
    return value == null ? null : MeleeWeapon.valueOf(value);
  }
}
