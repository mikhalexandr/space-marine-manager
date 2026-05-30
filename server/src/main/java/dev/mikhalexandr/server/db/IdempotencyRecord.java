package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.dto.response.CommandResponse;

/**
 * Сохранённая запись идемпотентности
 *
 * @param requestHash хэш запроса (тип команды + payload)
 * @param status статус обработки
 * @param response сохранённый ответ команды или {@code null}
 */
public record IdempotencyRecord(String requestHash, String status, CommandResponse response) {
  public static final String STATUS_PROCESSING = "PROCESSING";
  public static final String STATUS_DONE = "DONE";

  public boolean isDone() {
    return STATUS_DONE.equals(status);
  }
}
