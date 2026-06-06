package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.dto.response.CommandResponse;

public record IdempotencyRecord(String requestHash, String status, CommandResponse response) {
  public static final String STATUS_PROCESSING = "PROCESSING";
  public static final String STATUS_DONE = "DONE";

  public boolean isDone() {
    return STATUS_DONE.equals(status);
  }
}
