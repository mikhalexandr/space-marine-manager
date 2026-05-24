package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.auth.AuthService;
import dev.mikhalexandr.server.managers.CommandExecutor;

public final class AuthenticatingInterceptor implements CommandInterceptor {
  private static final String UNAUTHORIZED_MESSAGE =
      "Требуется авторизация: выполните вход или регистрацию.";

  private final AuthService authService;

  public AuthenticatingInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public CommandResponse intercept(CommandRequest request, CommandExecutor next) {
    CommandType commandType = request.getCommandType();
    if (commandType == CommandType.REGISTER) {
      return authService.register(request.getCredentials());
    }
    if (commandType == CommandType.LOGIN) {
      return authService.login(request.getCredentials());
    }
    if (!authService.isAuthenticated(request.getCredentials())) {
      return CommandResponse.error(UNAUTHORIZED_MESSAGE);
    }
    return next.execute(request);
  }
}
