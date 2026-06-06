package dev.mikhalexandr.common.dto.event;

import dev.mikhalexandr.common.models.SpaceMarine;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public final class CollectionEvent implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public enum Type {
    ADDED,
    UPDATED,
    REMOVED
  }

  private final Type type;
  private final SpaceMarine marine;
  private final List<Long> removedIds;

  private CollectionEvent(Type type, SpaceMarine marine, List<Long> removedIds) {
    this.type = type;
    this.marine = marine;
    this.removedIds = removedIds == null ? List.of() : List.copyOf(removedIds);
  }

  public static CollectionEvent added(SpaceMarine marine) {
    return new CollectionEvent(Type.ADDED, marine, List.of());
  }

  public static CollectionEvent updated(SpaceMarine marine) {
    return new CollectionEvent(Type.UPDATED, marine, List.of());
  }

  public static CollectionEvent removed(List<Long> removedIds) {
    return new CollectionEvent(Type.REMOVED, null, removedIds);
  }

  public Type getType() {
    return type;
  }

  public SpaceMarine getMarine() {
    return marine;
  }

  public List<Long> getRemovedIds() {
    return removedIds;
  }
}
