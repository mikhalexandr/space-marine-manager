package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.auth.AuthService;
import dev.mikhalexandr.server.managers.CommandExecutor;

/**
 * Proxy, обеспечивающий регистрацию/авторизацию
 */
public final class AuthenticatingCommandExecutorProxy implements CommandExecutor {
  private static final String UNAUTHORIZED_MESSAGE =
      "Требуется авторизация: выполните вход или регистрацию.";

  private final CommandExecutor delegate;
  private final AuthService authService;

  /**
   * @param delegate целевой исполнитель команд
   * @param authService сервис аутентификации
   */
  public AuthenticatingCommandExecutorProxy(CommandExecutor delegate, AuthService authService) {
    this.delegate = delegate;
    this.authService = authService;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
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
    return delegate.execute(request);
  }
}
