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

public final class JdbcSpaceMarineRepository implements SpaceMarineRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSpaceMarineRepository.class);

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

  public JdbcSpaceMarineRepository(Database database) {
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
  public boolean deleteById(int id) {
    return database.execute(connection -> doDeleteById(connection, id));
  }

  @Override
  public int deleteByOwner(String ownerLogin) {
    return database.execute(connection -> doDeleteByOwner(connection, ownerLogin));
  }

  @Override
  public List<SpaceMarine> findAll() {
    return database.execute(JdbcSpaceMarineRepository::doFindAll);
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
