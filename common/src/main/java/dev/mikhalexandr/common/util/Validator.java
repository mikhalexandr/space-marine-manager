package dev.mikhalexandr.common.util;

import dev.mikhalexandr.common.exceptions.ValidationException;

public final class Validator {
  private Validator() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static boolean isValidString(String str) {
    return str != null && !str.trim().isEmpty();
  }

  public static boolean isGreaterThan(double value, double min) {
    return value > min;
  }

  public static void validateString(String s, String fieldName) {
    if (s == null || s.trim().isEmpty()) {
      throw new ValidationException(String.format("%s не может быть null или пустым", fieldName));
    }
  }

  public static void validateGreaterThan(double value, double min, String fieldName) {
    if (value <= min) {
      throw new ValidationException(
          String.format("%s должен быть больше %s, получено: %s", fieldName, min, value));
    }
  }

  public static void validateNotNull(Object value, String fieldName) {
    if (value == null) {
      throw new ValidationException(String.format("%s не может быть null", fieldName));
    }
  }

  public static void validateId(Integer id) {
    if (id == null || id <= 0) {
      throw new ValidationException(String.format("id должен быть больше 0, получено: %s", id));
    }
  }
}
