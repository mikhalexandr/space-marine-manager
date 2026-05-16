package dev.mikhalexandr.common.dto.request.payload;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.io.Serial;

/** Payload для update: id + новый объект SpaceMarine. */
public final class IdMarinePayload implements CommandPayload {
  @Serial private static final long serialVersionUID = 1L;

  private final int id;
  private final SpaceMarine spaceMarine;

  public IdMarinePayload(int id, SpaceMarine spaceMarine) {
    this.id = id;
    this.spaceMarine = spaceMarine;
  }

  public int getId() {
    return id;
  }

  public SpaceMarine getSpaceMarine() {
    return spaceMarine;
  }
}
