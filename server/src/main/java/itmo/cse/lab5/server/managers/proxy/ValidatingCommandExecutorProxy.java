package itmo.cse.lab5.server.managers.proxy;

import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.dto.response.CommandResponse;
import itmo.cse.lab5.common.util.Validator;
import itmo.cse.lab5.server.managers.CommandExecutor;

/**
 * Proxy для валидации входящего запроса команды.
 */
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

        CommandRequest normalizedRequest = new CommandRequest(
                request.getCommandName().trim().toLowerCase(),
                request.getCommandArguments()
        );
        return delegate.execute(normalizedRequest);
    }

    private CommandResponse validateRequest(CommandRequest request) {
        if (request == null) {
            return CommandResponse.failure("Пустой запрос команды");
        }
        if (!Validator.isValidString(request.getCommandName())) {
            return CommandResponse.failure("Имя команды не может быть пустым");
        }

        String[] arguments = request.getCommandArguments();
        for (String argument : arguments) {
            if (argument == null) {
                return CommandResponse.failure("Аргументы команды не могут содержать null");
            }
        }

        return null;
    }
}
