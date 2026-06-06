package dev.mikhalexandr.server.db;

import dev.mikhalexandr.common.dto.response.CommandResponse;
import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {
  void claim(String userId, String requestId, String requestHash);

  void complete(String userId, String requestId, CommandResponse response);

  Optional<IdempotencyRecord> find(String userId, String requestId);

  int deleteExpired(Duration retention);
}
