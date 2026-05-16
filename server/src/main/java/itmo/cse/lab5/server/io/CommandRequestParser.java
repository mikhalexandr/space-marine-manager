package itmo.cse.lab5.server.io;

import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.util.Validator;

/**
 * Преобразует сырую строку команды в {@link CommandRequest}.
 */
public class CommandRequestParser {
    /**
     * @param rawCommand строка пользовательского ввода
     * @return DTO запроса или {@code null}, если строка пуста/невалидна
     */
    public CommandRequest toRequest(String rawCommand) {
        if (!Validator.isValidString(rawCommand)) {
            return null;
        }

        String normalizedInput = rawCommand.trim();
        String[] parts = normalizedInput.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();

        if (!Validator.isValidString(commandName)) {
            return null;
        }

        String[] commandArguments = new String[0];
        if (parts.length > 1 && Validator.isValidString(parts[1])) {
            commandArguments = new String[]{parts[1].trim()};
        }

        return new CommandRequest(commandName, commandArguments);
    }
}
