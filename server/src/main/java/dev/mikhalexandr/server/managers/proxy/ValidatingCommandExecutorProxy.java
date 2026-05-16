package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandExecutor;

/** Proxy для валидации входящего запроса команды. */
public class ValidatingCommandExecutorProxy implements CommandExecutor {
  private final CommandExecutor delegate;

  /**
   * @param delegate целевой исполнитель команд
   */
  public ValidatingCommandExecutorProxy(CommandExecutor delegate) {
    this.delegate = delegate;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    CommandResponse validationFailure = validateRequest(request);
    if (validationFailure != null) {
      return validationFailure;
    }
    return delegate.execute(request);
  }

  private CommandResponse validateRequest(CommandRequest request) {
    if (request == null) {
      return CommandResponse.error("Пустой запрос команды");
    }
    if (request.getCommandType() == null) {
      return CommandResponse.error("Тип команды не может быть пустым");
    }

    return null;
  }
}
