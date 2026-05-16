package dev.mikhalexandr.client.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.request.payload.CategoryPayload;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.request.payload.NoArgsPayload;
import dev.mikhalexandr.common.dto.request.payload.RawArgumentsPayload;
import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.util.Validator;
import java.util.Arrays;

/** Преобразует пользовательский ввод клиента в объектный CommandRequest. */
public class CommandRequestParser {
  /**
   * @param rawCommand исходная строка из консоли или скрипта
   * @return объектный запрос или null для пустого ввода
   */
  public CommandRequest parse(String rawCommand) {
    if (!Validator.isValidString(rawCommand)) {
      return null;
    }

    String[] parts = rawCommand.trim().split("\\s+", 2);
    String commandName = parts[0].trim().toLowerCase();
    String[] arguments =
        parts.length == 2 && Validator.isValidString(parts[1])
            ? new String[] {parts[1].trim()}
            : new String[] {};

    CommandType commandType = CommandType.fromWireName(commandName);
    return switch (commandType) {
      case HELP,
              INFO,
              SHOW,
              CLEAR,
              HEAD,
              SUM_OF_HEALTH,
              MAX_BY_CHAPTER,
              HISTORY,
              ADD,
              ADD_IF_MIN,
              EXIT,
              EXECUTE_SCRIPT ->
          new CommandRequest(commandType, NoArgsPayload.INSTANCE);
      case REMOVE_BY_ID, UPDATE -> buildIdBasedRequest(commandType, arguments);
      case COUNT_BY_CATEGORY -> buildCategoryRequest(arguments);
      case UNKNOWN -> buildUnknownRequest(commandName, arguments);
    };
  }

  private CommandRequest buildUnknownRequest(String commandName, String[] arguments) {
    String[] payloadArguments = new String[arguments.length + 1];
    payloadArguments[0] = commandName;
    System.arraycopy(arguments, 0, payloadArguments, 1, arguments.length);
    String[] normalized =
        Arrays.stream(payloadArguments).filter(Validator::isValidString).toArray(String[]::new);
    return new CommandRequest(CommandType.UNKNOWN, new RawArgumentsPayload(normalized));
  }

  private CommandRequest buildIdBasedRequest(CommandType commandType, String[] arguments) {
    if (arguments.length == 0) {
      return new CommandRequest(commandType, new RawArgumentsPayload(arguments));
    }
    try {
      int id = Integer.parseInt(arguments[0]);
      return new CommandRequest(commandType, new IdPayload(id));
    } catch (NumberFormatException e) {
      return new CommandRequest(commandType, new RawArgumentsPayload(arguments));
    }
  }

  private CommandRequest buildCategoryRequest(String[] arguments) {
    if (arguments.length == 0) {
      return new CommandRequest(CommandType.COUNT_BY_CATEGORY, new RawArgumentsPayload(arguments));
    }
    try {
      AstartesCategory category = AstartesCategory.valueOf(arguments[0].trim().toUpperCase());
      return new CommandRequest(CommandType.COUNT_BY_CATEGORY, new CategoryPayload(category));
    } catch (IllegalArgumentException e) {
      return new CommandRequest(CommandType.COUNT_BY_CATEGORY, new RawArgumentsPayload(arguments));
    }
  }
}
