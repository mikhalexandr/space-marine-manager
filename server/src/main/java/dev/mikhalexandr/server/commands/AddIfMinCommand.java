package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.MarinePayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code add_if_min}: добавляет элемент, если он меньше минимального. */
public class AddIfMinCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public AddIfMinCommand(CollectionManager collectionManager) {
    super("add_if_min", "{element}", "добавить элемент, если он меньше минимального");
    this.collectionManager = collectionManager;
  }

  /**
   * Добавляет элемент по условию {@code add_if_min} (из payload или интерактивного ввода).
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если не удалось получить данные объекта
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    SpaceMarine spaceMarine = extractMarine(request);
    if (collectionManager.addIfMin(spaceMarine)) {
      return CommandResponse.success("Элемент добавлен");
    }
    return CommandResponse.success("Элемент НЕ добавлен, так как он НЕ меньше минимального");
  }

  private static SpaceMarine extractMarine(CommandRequest request)
      throws CommandExecutionException {
    if (request == null || !(request.getPayload() instanceof MarinePayload payload)) {
      throw new CommandExecutionException("Для add_if_min требуется объектный payload SpaceMarine");
    }
    SpaceMarine spaceMarine = payload.getSpaceMarine();
    if (spaceMarine == null) {
      throw new CommandExecutionException("Для add_if_min требуется объект SpaceMarine");
    }
    return spaceMarine;
  }
}
