package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.IdMarinePayload;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code update}: обновляет элемент коллекции по id. */
public class UpdateCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public UpdateCommand(CollectionManager collectionManager) {
    super("update", "<id> {element}", "обновить элемент коллекции по id");
    this.collectionManager = collectionManager;
  }

  /**
   * Обновляет элемент по DTO-запросу (payload или legacy-путь).
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если id не передан, некорректен или элемент не найден
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    if (request == null || request.getPayload() == null) {
      throw new CommandExecutionException("Для update требуется payload с id");
    }

    if (request.getPayload() instanceof IdPayload idPayload) {
      SpaceMarine existing = collectionManager.getById(idPayload.getId());
      if (existing == null) {
        throw new CommandExecutionException(
            String.format("Элемент с id=%d не найден", idPayload.getId()));
      }
      return CommandResponse.success("id существует");
    }

    if (!(request.getPayload() instanceof IdMarinePayload payload)) {
      throw new CommandExecutionException("Для update требуется payload вида {id + SpaceMarine}");
    }

    SpaceMarine spaceMarine = payload.getSpaceMarine();
    if (spaceMarine == null) {
      throw new CommandExecutionException("Для update требуется объект SpaceMarine");
    }

    SpaceMarine existing = collectionManager.getById(payload.getId());
    if (existing == null) {
      throw new CommandExecutionException(
          String.format("Элемент с id=%d не найден", payload.getId()));
    }

    collectionManager.update(payload.getId(), spaceMarine);
    return CommandResponse.success(
        String.format(
            "Элемент успешно обновлён%nНовое значение элемента:%n%s",
            collectionManager.getById(payload.getId())));
  }
}
