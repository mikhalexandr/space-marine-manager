package dev.mikhalexandr.common.dto.request;

import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.request.payload.CommandPayload;
import dev.mikhalexandr.common.dto.request.payload.NoArgsPayload;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public final class CommandRequest implements Serializable {
  @Serial private static final long serialVersionUID = 3L;

  private final String requestId;
  private final CommandType commandType;
  private final CommandPayload payload;
  private UserCredentials credentials;

  public CommandRequest(CommandType commandType, CommandPayload payload) {
    this(commandType, payload, UUID.randomUUID().toString());
  }

  public CommandRequest(CommandType commandType, CommandPayload payload, String requestId) {
    this.requestId =
        requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    this.commandType = commandType == null ? CommandType.UNKNOWN : commandType;
    this.payload = payload == null ? NoArgsPayload.INSTANCE : payload;
  }

  public String getRequestId() {
    return requestId;
  }

  public CommandType getCommandType() {
    return commandType;
  }

  public CommandPayload getPayload() {
    return payload;
  }

  public UserCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(UserCredentials credentials) {
    this.credentials = credentials;
  }
}
