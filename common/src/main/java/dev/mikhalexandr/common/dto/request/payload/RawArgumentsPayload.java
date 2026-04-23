package dev.mikhalexandr.common.dto.request.payload;

import java.io.Serial;
import java.util.Arrays;

/** Универсальный payload для поэтапной миграции со строковых аргументов. */
public final class RawArgumentsPayload implements CommandPayload {
  @Serial private static final long serialVersionUID = 1L;

  private final String[] arguments;

  public RawArgumentsPayload(String[] arguments) {
    this.arguments =
        arguments == null ? new String[] {} : Arrays.copyOf(arguments, arguments.length);
  }

  public String[] getArguments() {
    return Arrays.copyOf(arguments, arguments.length);
  }
}
