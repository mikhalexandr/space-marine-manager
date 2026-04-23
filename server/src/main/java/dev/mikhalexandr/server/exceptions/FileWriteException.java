package dev.mikhalexandr.server.exceptions;

/** Ошибка записи данных в файл. */
public class FileWriteException extends RuntimeException {
  /**
   * @param message текст ошибки
   * @param cause исходная причина
   */
  public FileWriteException(String message, Throwable cause) {
    super(message, cause);
  }
}
