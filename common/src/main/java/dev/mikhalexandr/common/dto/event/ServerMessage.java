package dev.mikhalexandr.common.dto.event;

import dev.mikhalexandr.common.dto.response.CommandResponse;
import java.io.Serial;
import java.io.Serializable;

public final class ServerMessage implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public enum Kind {
    RESPONSE,
    EVENT
  }

  private final Kind kind;
  private final String correlationId;
  private final CommandResponse response;
  private final CollectionEvent event;

  private ServerMessage(
      Kind kind, String correlationId, CommandResponse response, CollectionEvent event) {
    this.kind = kind;
    this.correlationId = correlationId;
    this.response = response;
    this.event = event;
  }

  public static ServerMessage response(String correlationId, CommandResponse response) {
    return new ServerMessage(Kind.RESPONSE, correlationId, response, null);
  }

  public static ServerMessage event(CollectionEvent event) {
    return new ServerMessage(Kind.EVENT, null, null, event);
  }

  public Kind getKind() {
    return kind;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public CommandResponse getResponse() {
    return response;
  }

  public CollectionEvent getEvent() {
    return event;
  }
}
