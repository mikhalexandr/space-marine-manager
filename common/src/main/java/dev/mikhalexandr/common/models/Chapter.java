package dev.mikhalexandr.common.models;

import dev.mikhalexandr.common.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Дополнительная информация об ордене Space Marine. */
public class Chapter implements Comparable<Chapter>, Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final String name;
  private final String parentLegion;

  /**
   * @param name имя ордена (не null и не пустое)
   * @param parentLegion родительский легион (может быть null)
   */
  public Chapter(String name, String parentLegion) {
    Validator.validateString(name, "Chapter.name");
    this.name = name;
    this.parentLegion = parentLegion;
  }

  /**
   * @return имя ордена
   */
  public String getName() {
    return name;
  }

  @Override
  public int compareTo(Chapter o) {
    return this.name.compareTo(o.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Chapter that = (Chapter) o;
    return Objects.equals(name, that.name) && Objects.equals(parentLegion, that.parentLegion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, parentLegion);
  }

  @Override
  public String toString() {
    return String.format("Chapter[name=%s, parentLegion=%s]", name, parentLegion);
  }
}
