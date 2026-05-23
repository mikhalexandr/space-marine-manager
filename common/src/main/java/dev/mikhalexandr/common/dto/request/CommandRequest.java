package dev.mikhalexandr.common.dto.request;

import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.request.payload.CommandPayload;
import dev.mikhalexandr.common.dto.request.payload.NoArgsPayload;
import java.io.Serial;
import java.io.Serializable;

/** Запрос на выполнение команды */
public final class CommandRequest implements Serializable {
  @Serial private static final long serialVersionUID = 2L;

  private final CommandType commandType;
  private final CommandPayload payload;
  private UserCredentials credentials;

  /**
   * Создает объектный запрос (тип + payload)
   *
   * @param commandType тип команды
   * @param payload объект аргументов
   */
  public CommandRequest(CommandType commandType, CommandPayload payload) {
    this.commandType = commandType == null ? CommandType.UNKNOWN : commandType;
    this.payload = payload == null ? NoArgsPayload.INSTANCE : payload;
  }

  /**
   * @return тип команды
   */
  public CommandType getCommandType() {
    return commandType;
  }

  /**
   * @return объектный payload
   */
  public CommandPayload getPayload() {
    return payload;
  }

  /**
   * @return учётные данные пользователя или null, если не заданы
   */
  public UserCredentials getCredentials() {
    return credentials;
  }

  /**
   * Прикрепляет учётные данные к запросу
   *
   * @param credentials учётные данные пользователя
   */
  public void setCredentials(UserCredentials credentials) {
    this.credentials = credentials;
  }
}
