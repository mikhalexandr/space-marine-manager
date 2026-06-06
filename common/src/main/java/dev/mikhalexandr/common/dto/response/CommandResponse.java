package dev.mikhalexandr.common.dto.response;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.io.Serializable;
import java.util.List;

public final class CommandResponse implements Serializable {
  private final boolean success;
  private final String message;
  private final List<SpaceMarine> data;

  public CommandResponse(boolean success, String message, List<SpaceMarine> data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  public static CommandResponse success(String message) {
    return new CommandResponse(true, message, null);
  }

  public static CommandResponse success(String message, List<SpaceMarine> data) {
    return new CommandResponse(true, message, data);
  }

  public static CommandResponse error(String message) {
    return new CommandResponse(false, message, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public List<SpaceMarine> getData() {
    return data;
  }
}
