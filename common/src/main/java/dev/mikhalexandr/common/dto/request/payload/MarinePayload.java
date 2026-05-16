package dev.mikhalexandr.common.dto.request.payload;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.io.Serial;

/** Payload с объектом SpaceMarine. */
public final class MarinePayload implements CommandPayload {
  @Serial private static final long serialVersionUID = 1L;

  private final SpaceMarine spaceMarine;

  public MarinePayload(SpaceMarine spaceMarine) {
    this.spaceMarine = spaceMarine;
  }

  public SpaceMarine getSpaceMarine() {
    return spaceMarine;
  }
}
