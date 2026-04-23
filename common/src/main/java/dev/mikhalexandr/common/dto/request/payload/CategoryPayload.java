package dev.mikhalexandr.common.dto.request.payload;

import dev.mikhalexandr.common.models.AstartesCategory;
import java.io.Serial;

/** Payload с категорией космодесантника. */
public final class CategoryPayload implements CommandPayload {
  @Serial private static final long serialVersionUID = 1L;

  private final AstartesCategory category;

  public CategoryPayload(AstartesCategory category) {
    this.category = category;
  }

  public AstartesCategory getCategory() {
    return category;
  }
}
