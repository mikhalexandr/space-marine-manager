package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandExecutor;

public final class ValidatingInterceptor implements CommandInterceptor {
  @Override
  public CommandResponse intercept(CommandRequest request, CommandExecutor next) {
    if (request == null) {
      return CommandResponse.error("Пустой запрос команды");
    }
    return next.execute(request);
  }
}
