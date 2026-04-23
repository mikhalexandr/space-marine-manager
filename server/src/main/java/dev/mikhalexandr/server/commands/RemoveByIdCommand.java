package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code remove_by_id}: удаляет элемент коллекции по id. */
public class RemoveByIdCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public RemoveByIdCommand(CollectionManager collectionManager) {
    super("remove_by_id", "<id>", "удалить элемент по id");
    this.collectionManager = collectionManager;
  }

  /**
   * Удаляет элемент по идентификатору.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если id не передан или имеет неверный формат
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    int id = resolveId(request);
    if (collectionManager.removeById(id)) {
      return CommandResponse.success("Элемент удалён");
    }
    return CommandResponse.success(String.format("Элемент с id=%d не найден", id));
  }

  private int resolveId(CommandRequest request) throws CommandExecutionException {
    IdPayload idPayload =
        CommandPayloads.requireIdPayload(
            request,
            () -> new CommandExecutionException("Для remove_by_id требуется payload с полем id"));
    return idPayload.getId();
  }
}
