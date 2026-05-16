package itmo.cse.lab5.server.managers.proxy;

import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.dto.response.CommandResponse;
import itmo.cse.lab5.server.managers.CommandExecutor;

import java.util.logging.Logger;

/**
 * Proxy для логирования выполнения команд.
 */
public class LoggingCommandExecutorProxy implements CommandExecutor {
    private static final long NANOS_IN_MILLISECOND = 1_000_000L;
    private static final Logger LOGGER = Logger.getLogger(LoggingCommandExecutorProxy.class.getName());
    private final CommandExecutor delegate;

    /**
     * @param delegate целевой исполнитель команд
     */
    public LoggingCommandExecutorProxy(CommandExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public CommandResponse execute(CommandRequest request) {
        String commandName = request == null || request.getCommandName() == null
                ? "<unknown>"
                : request.getCommandName();

        long startTime = System.nanoTime();
        CommandResponse response = delegate.execute(request);
        long executionMs = (System.nanoTime() - startTime) / NANOS_IN_MILLISECOND;

        LOGGER.info(() -> String.format(
                "Команда '%s' выполнена за %d мс (success=%s)",
                commandName,
                executionMs,
                response.isSuccess()
        ));

        return response;
    }
}
