package dev.mikhalexandr.common.dto.request;

import dev.mikhalexandr.common.dto.request.payload.CommandPayload;
import dev.mikhalexandr.common.dto.request.payload.NoArgsPayload;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/** Запрос на выполнение команды. */
public final class CommandRequest implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final String requestId;
  private final CommandType commandType;
  private final CommandPayload payload;

  /**
   * Создает объектный запрос (тип + payload).
   *
   * @param commandType тип команды
   * @param payload объект аргументов
   */
  public CommandRequest(CommandType commandType, CommandPayload payload) {
    this.requestId = UUID.randomUUID().toString();
    this.commandType = commandType == null ? CommandType.UNKNOWN : commandType;
    this.payload = payload == null ? NoArgsPayload.INSTANCE : payload;
  }

  /**
   * @return идентификатор запроса для корреляции с ответом
   */
  public String getRequestId() {
    return requestId;
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
}
