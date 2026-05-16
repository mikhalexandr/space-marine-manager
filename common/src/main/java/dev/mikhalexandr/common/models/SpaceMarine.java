package dev.mikhalexandr.common.models;

import dev.mikhalexandr.common.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Модель элемента коллекции {@code SpaceMarine}. Реализует естественный порядок сортировки по
 * имени.
 */
public class SpaceMarine implements Comparable<SpaceMarine>, Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private static final int MIN_X = -121;
  private static final int MIN_Y = -184;

  private Integer id;
  private final String name;
  private final Coordinates coordinates;
  private java.util.Date creationDate;
  private final float health;
  private final long height;
  private final AstartesCategory category;
  private final MeleeWeapon meleeWeapon;
  private final Chapter chapter;

  /**
   * Создает новый экземпляр SpaceMarine.
   *
   * @param name имя (не null и не пустое)
   * @param coordinates координаты (не null)
   * @param health здоровье (должно быть > 0)
   * @param height рост
   * @param category категория (не null)
   * @param meleeWeapon оружие ближнего боя (может быть null)
   * @param chapter информация об ордене (может быть null)
   */
  public SpaceMarine(
      String name,
      Coordinates coordinates,
      float health,
      long height,
      AstartesCategory category,
      MeleeWeapon meleeWeapon,
      Chapter chapter) {
    Validator.validateString(name, "SpaceMarine.name");
    Validator.validateNotNull(coordinates, "SpaceMarine.coordinates");
    Validator.validateGreaterThan(health, 0, "SpaceMarine.health");
    Validator.validateNotNull(category, "SpaceMarine.category");
    this.name = name;
    this.coordinates = coordinates;
    this.health = health;
    this.height = height;
    this.category = category;
    this.meleeWeapon = meleeWeapon;
    this.chapter = chapter;
  }

  /** Проверяет корректность состояния объекта. */
  public void validate() {
    Validator.validateId(id);
    Validator.validateString(name, "SpaceMarine.name");
    Validator.validateNotNull(coordinates, "SpaceMarine.coordinates");
    Validator.validateGreaterThan(coordinates.getX(), MIN_X, "Coordinates.x");
    Validator.validateGreaterThan(coordinates.getY(), MIN_Y, "Coordinates.y");
    Validator.validateNotNull(creationDate, "SpaceMarine.creationDate");
    Validator.validateGreaterThan(health, 0, "SpaceMarine.health");
    Validator.validateNotNull(category, "SpaceMarine.category");
    if (chapter != null) {
      Validator.validateString(chapter.getName(), "Chapter.name");
    }
  }

  /**
   * @return идентификатор объекта
   */
  public Integer getId() {
    return id;
  }

  /**
   * @param id идентификатор (должен быть > 0)
   */
  public void setId(Integer id) {
    Validator.validateId(id);
    this.id = id;
  }

  /**
   * @return имя SpaceMarine
   */
  public String getName() {
    return name;
  }

  /**
   * @return дата создания объекта
   */
  public java.util.Date getCreationDate() {
    return creationDate;
  }

  /**
   * @param creationDate дата создания (не null)
   */
  public void setCreationDate(java.util.Date creationDate) {
    Validator.validateNotNull(creationDate, "SpaceMarine.creationDate");
    this.creationDate = creationDate;
  }

  /**
   * @return значение здоровья
   */
  public float getHealth() {
    return health;
  }

  /**
   * @return категория космодесантника
   */
  public AstartesCategory getCategory() {
    return category;
  }

  /**
   * @return данные ордена или null
   */
  public Chapter getChapter() {
    return chapter;
  }

  @Override
  public int compareTo(SpaceMarine o) {
    return Float.compare(this.health, o.health);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpaceMarine that = (SpaceMarine) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    String separator = "═══════════════════════════════════";
    String line = "───────────────────────────────────";
    return String.format(
        """
                       %s
                        [%d] %s%n%s
                         Coordinates:  (%.1f; %d)
                         Created:      %s
                         Health:       %.1f
                         Height:       %d
                         Category:     %s
                         Melee Weapon: %s
                         Chapter:      %s""",
        separator,
        id,
        name,
        line,
        coordinates.getX(),
        coordinates.getY(),
        new java.text.SimpleDateFormat("HH:mm:ss dd.MM.yyyy").format(creationDate),
        health,
        height,
        category,
        meleeWeapon != null ? meleeWeapon : "-",
        chapter != null ? chapter.getName() : "-");
  }
}
