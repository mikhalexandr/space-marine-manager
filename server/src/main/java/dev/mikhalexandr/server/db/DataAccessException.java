package dev.mikhalexandr.server.db;

/** Ошибка доступа к бдхе */
public class DataAccessException extends RuntimeException {
  /**
   * @param message текст ошибки
   * @param cause исходная причина
   */
  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
