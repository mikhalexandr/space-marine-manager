package dev.mikhalexandr.server.exceptions;

public class IdempotencyConflictException extends RuntimeException {
  /**
   * @param message текст ошибки
   */
  public IdempotencyConflictException(String message) {
    super(message);
  }
}
