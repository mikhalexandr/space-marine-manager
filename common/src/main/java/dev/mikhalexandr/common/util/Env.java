package dev.mikhalexandr.common.util;

/** Утилитка для чтения переменных окружения. */
public final class Env {

  private Env() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * @param name имя переменной окружения
   * @param fallback значение по умолчанию
   * @return значение из env или fallback
   */
  public static String orDefault(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
