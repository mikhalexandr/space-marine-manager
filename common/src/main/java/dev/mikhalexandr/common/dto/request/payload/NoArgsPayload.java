package dev.mikhalexandr.common.dto.request.payload;

import java.io.Serial;

public final class NoArgsPayload implements CommandPayload {
  public static final NoArgsPayload INSTANCE = new NoArgsPayload();

  @Serial private static final long serialVersionUID = 1L;

  private NoArgsPayload() {}
}
