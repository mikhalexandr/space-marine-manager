package dev.mikhalexandr.server.exceptions;

/** Ошибка выполнения пользовательской команды. */
public class CommandExecutionException extends Exception {
  /**
   * @param message текст ошибки
   */
  public CommandExecutionException(String message) {
    super(message);
  }
}
