package dev.mikhalexandr.server.exceptions;

/** Ошибка чтения данных из файла. */
public class FileReadException extends RuntimeException {
  /**
   * @param message текст ошибки
   * @param cause исходная причина
   */
  public FileReadException(String message, Throwable cause) {
    super(message, cause);
  }
}
