package dev.mikhalexandr.common.dto.request.payload;

import java.io.Serial;

/** Payload с числовым id. */
public final class IdPayload implements CommandPayload {
  @Serial private static final long serialVersionUID = 1L;

  private final int id;

  public IdPayload(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }
}
