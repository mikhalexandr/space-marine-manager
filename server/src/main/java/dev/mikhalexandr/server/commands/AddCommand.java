package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.MarinePayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code add}: добавляет новый элемент в коллекцию. */
public class AddCommand extends Command {
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  /**
   * @param collectionManager менеджер коллекции в памяти
   * @param repository репозиторий коллекции в БД
   */
  public AddCommand(CollectionManager collectionManager, SpaceMarineRepository repository) {
    super("add", "{element}", "добавить новый элемент в коллекцию");
    this.collectionManager = collectionManager;
    this.repository = repository;
  }

  /**
   * Сохраняет элемент в БД, затем — только при успехе — добавляет его в коллекцию в памяти.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если payload не содержит объект SpaceMarine
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    SpaceMarine spaceMarine = extractMarine(request);
    repository.insert(spaceMarine, currentUser(request));
    collectionManager.add(spaceMarine);
    return CommandResponse.success(
        String.format("Элемент успешно добавлен (id=%d)", spaceMarine.getId()));
  }

  private static SpaceMarine extractMarine(CommandRequest request)
      throws CommandExecutionException {
    MarinePayload payload =
        CommandPayloads.requireMarinePayload(
            request,
            () -> new CommandExecutionException("Для add требуется объектный payload SpaceMarine"));
    SpaceMarine spaceMarine = payload.getSpaceMarine();
    if (spaceMarine == null) {
      throw new CommandExecutionException("Для add требуется объект SpaceMarine");
    }
    return spaceMarine;
  }
}
