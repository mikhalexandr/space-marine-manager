package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandExecutor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class IdempotencyInterceptor implements CommandInterceptor {
  private final Map<String, CommandResponse> processed = new ConcurrentHashMap<>();

  @Override
  public CommandResponse intercept(CommandRequest request, CommandExecutor next) {
    String requestId = request.getRequestId();
    if (requestId == null || requestId.isBlank()) {
      return next.execute(request);
    }
    return processed.computeIfAbsent(requestId, id -> next.execute(request));
  }
}
