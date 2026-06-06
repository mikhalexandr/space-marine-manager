package dev.mikhalexandr.common.models;

import dev.mikhalexandr.common.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class Coordinates implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private static final int MIN_X = -121;
  private static final int MIN_Y = -184;

  private final double x;
  private final long y;

  public Coordinates(double x, long y) {
    Validator.validateGreaterThan(x, MIN_X, "Coordinates.x");
    Validator.validateGreaterThan(y, MIN_Y, "Coordinates.y");
    this.x = x;
    this.y = y;
  }

  public double getX() {
    return x;
  }

  public long getY() {
    return y;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Coordinates that = (Coordinates) o;
    return Objects.equals(x, that.x) && Objects.equals(y, that.y);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public String toString() {
    return String.format("Coordinates[x=%f,y=%d]", x, y);
  }
}
