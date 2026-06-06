package dev.mikhalexandr.server.managers;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;

public interface CommandExecutor {
  CommandResponse execute(CommandRequest request);
}
