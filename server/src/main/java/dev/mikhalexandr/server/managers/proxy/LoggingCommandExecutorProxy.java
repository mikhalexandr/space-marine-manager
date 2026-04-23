package dev.mikhalexandr.server.managers.proxy;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Proxy для логирования выполнения команд. */
public class LoggingCommandExecutorProxy implements CommandExecutor {
  private static final long NANOS_IN_MILLISECOND = 1_000_000L;
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCommandExecutorProxy.class);
  private final CommandExecutor delegate;

  /**
   * @param delegate целевой исполнитель команд
   */
  public LoggingCommandExecutorProxy(CommandExecutor delegate) {
    this.delegate = delegate;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    String commandName = request == null ? "<unknown>" : request.getCommandType().getWireName();

    long startTime = System.nanoTime();
    CommandResponse response = delegate.execute(request);
    long executionMs = (System.nanoTime() - startTime) / NANOS_IN_MILLISECOND;

    if (response.isSuccess()) {
      LOGGER.info("Команда '{}' успешно обработана за {} мс", commandName, executionMs);
    } else {
      LOGGER.info("Команда '{}' завершилась с ошибкой за {} мс", commandName, executionMs);
    }

    return response;
  }
}
