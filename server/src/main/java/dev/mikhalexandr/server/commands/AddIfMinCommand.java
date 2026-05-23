package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.MarinePayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code add_if_min}: добавляет элемент, если он меньше минимального. */
public class AddIfMinCommand extends Command {
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  /**
   * @param collectionManager менеджер коллекции в памяти
   * @param repository репозиторий коллекции в БД
   */
  public AddIfMinCommand(CollectionManager collectionManager, SpaceMarineRepository repository) {
    super("add_if_min", "{element}", "добавить элемент, если он меньше минимального");
    this.collectionManager = collectionManager;
    this.repository = repository;
  }

  /**
   * Добавляет элемент в БД и память по условию {@code add_if_min}
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если payload не содержит объект SpaceMarine
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    SpaceMarine spaceMarine = extractMarine(request);
    if (!collectionManager.isLessThanMin(spaceMarine)) {
      return CommandResponse.success("Элемент НЕ добавлен: он не меньше минимального");
    }
    repository.insert(spaceMarine, currentUser(request));
    collectionManager.add(spaceMarine);
    return CommandResponse.success(String.format("Элемент добавлен (id=%d)", spaceMarine.getId()));
  }

  private static SpaceMarine extractMarine(CommandRequest request)
      throws CommandExecutionException {
    MarinePayload payload =
        CommandPayloads.requireMarinePayload(
            request,
            () ->
                new CommandExecutionException(
                    "Для add_if_min требуется объектный payload SpaceMarine"));
    SpaceMarine spaceMarine = payload.getSpaceMarine();
    if (spaceMarine == null) {
      throw new CommandExecutionException("Для add_if_min требуется объект SpaceMarine");
    }
    return spaceMarine;
  }
}
