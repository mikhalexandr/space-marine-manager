package dev.mikhalexandr.common.models;

import dev.mikhalexandr.common.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class SpaceMarine implements Comparable<SpaceMarine>, Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Integer id;
  private final String name;
  private final Coordinates coordinates;
  private java.util.Date creationDate;
  private final float health;
  private final long height;
  private final AstartesCategory category;
  private final MeleeWeapon meleeWeapon;
  private final Chapter chapter;
  private String owner;

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

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    Validator.validateId(id);
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public java.util.Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(java.util.Date creationDate) {
    Validator.validateNotNull(creationDate, "SpaceMarine.creationDate");
    this.creationDate = creationDate;
  }

  public float getHealth() {
    return health;
  }

  public AstartesCategory getCategory() {
    return category;
  }

  public Chapter getChapter() {
    return chapter;
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public long getHeight() {
    return height;
  }

  public MeleeWeapon getMeleeWeapon() {
    return meleeWeapon;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
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
                         Chapter:      %s
                         Owner:        %s""",
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
        chapter != null ? chapter.getName() : "-",
        owner != null ? owner : "-");
  }
}
