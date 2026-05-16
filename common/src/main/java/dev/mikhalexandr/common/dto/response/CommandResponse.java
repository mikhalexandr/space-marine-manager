package dev.mikhalexandr.common.dto.response;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.io.Serializable;
import java.util.List;

/** Ответ на выполнение команды. */
public final class CommandResponse implements Serializable {
  private final boolean success;
  private final String message;
  private final List<SpaceMarine> data;

  /**
   * @param success признак успешного выполнения
   * @param message текст ответа
   * @param data объектные данные ответа
   */
  public CommandResponse(boolean success, String message, List<SpaceMarine> data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  /**
   * @param message текст ответа
   * @return успешный ответ
   */
  public static CommandResponse success(String message) {
    return new CommandResponse(true, message, null);
  }

  /**
   * @param message текст ответа
   * @param data объектные данные ответа
   * @return успешный ответ
   */
  public static CommandResponse success(String message, List<SpaceMarine> data) {
    return new CommandResponse(true, message, data);
  }

  /**
   * @param message текст ошибки
   * @return ответ с ошибкой
   */
  public static CommandResponse error(String message) {
    return new CommandResponse(false, message, null);
  }

  /**
   * @return признак успешности
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * @return текст ответа
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return объектные данные ответа или null
   */
  public List<SpaceMarine> getData() {
    return data;
  }
}
