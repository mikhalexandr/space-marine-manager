package itmo.cse.lab5.common.models;

import itmo.cse.lab5.common.util.Validator;

import java.util.Objects;

/**
 * Координаты космодесантника.
 */
public class Coordinates {
    private static final int MIN_X = -121;
    private static final int MIN_Y = -184;

    private final double x;
    private final long y;

    /**
     * @param x координата X (должна быть больше -121)
     * @param y координата Y (должна быть больше -184)
     */
    public Coordinates(double x, long y) {
        Validator.validateGreaterThan(x, MIN_X, "Coordinates.x");
        Validator.validateGreaterThan(y, MIN_Y, "Coordinates.y");
        this.x = x;
        this.y = y;
    }

    /**
     * @return значение координаты X
     */
    public double getX() {
        return x;
    }

    /**
     * @return значение координаты Y
     */
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
        return Objects.equals(x, that.x)
                && Objects.equals(y, that.y);
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
