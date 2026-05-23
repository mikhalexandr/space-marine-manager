package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.IdMarinePayload;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code update}: обновляет элемент коллекции по id */
public class UpdateCommand extends Command {
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  /**
   * @param collectionManager менеджер коллекции в памяти
   * @param repository репозиторий коллекции в БД
   */
  public UpdateCommand(CollectionManager collectionManager, SpaceMarineRepository repository) {
    super("update", "<id> {element}", "обновить свой элемент коллекции по id");
    this.collectionManager = collectionManager;
    this.repository = repository;
  }

  /**
   * Первый этап (payload только с id) проверяет существование и права; второй этап (id + объект)
   * применяет изменения в бдхе и памяти
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если payload некорректен или объект чужой/не найден
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    IdPayload idPayload = CommandPayloads.findIdPayload(request).orElse(null);
    if (idPayload != null) {
      ensureOwned(
          collectionManager.getById(idPayload.getId()), idPayload.getId(), currentUser(request));
      return CommandResponse.success("id существует");
    }
    IdMarinePayload payload =
        CommandPayloads.requireIdMarinePayload(
            request,
            () ->
                new CommandExecutionException(
                    "Для update требуется payload вида {id + SpaceMarine}"));
    return applyUpdate(payload, currentUser(request));
  }

  private CommandResponse applyUpdate(IdMarinePayload payload, String user)
      throws CommandExecutionException {
    SpaceMarine spaceMarine = payload.getSpaceMarine();
    if (spaceMarine == null) {
      throw new CommandExecutionException("Для update требуется объект SpaceMarine");
    }
    int id = payload.getId();
    ensureOwned(collectionManager.getById(id), id, user);
    if (!repository.update(id, spaceMarine)) {
      collectionManager.removeById(id);
      return CommandResponse.success(String.format("Элемент с id=%d уже удалён", id));
    }
    collectionManager.update(id, spaceMarine);
    return CommandResponse.success(
        String.format("Элемент успешно обновлён%n%s", collectionManager.getById(id)));
  }

  private static void ensureOwned(SpaceMarine existing, int id, String user)
      throws CommandExecutionException {
    if (existing == null) {
      throw new CommandExecutionException(String.format("Элемент с id=%d не найден", id));
    }
    if (!user.equals(existing.getOwner())) {
      throw new CommandExecutionException(
          String.format(
              "Элемент с id=%d принадлежит пользователю '%s' — изменять нельзя",
              id, existing.getOwner()));
    }
  }
}
