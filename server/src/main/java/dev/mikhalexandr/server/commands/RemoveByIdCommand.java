package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.CommandPayloads;
import dev.mikhalexandr.common.dto.request.payload.IdPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

public class RemoveByIdCommand extends Command {
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  public RemoveByIdCommand(CollectionManager collectionManager, SpaceMarineRepository repository) {
    super("remove_by_id", "<id>", "удалить свой элемент по id");
    this.collectionManager = collectionManager;
    this.repository = repository;
  }

  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    int id = resolveId(request);
    String user = currentUser(request);
    SpaceMarine existing = repository.findById(id).orElseGet(() -> collectionManager.getById(id));
    if (existing == null) {
      collectionManager.removeById(id);
      return CommandResponse.success(String.format("Элемент с id=%d не найден", id));
    }
    assert user != null;
    if (!user.equals(existing.getOwner())) {
      throw new CommandExecutionException(
          String.format(
              "Элемент с id=%d принадлежит пользователю '%s' — удалять нельзя",
              id, existing.getOwner()));
    }
    boolean removedFromDb = repository.deleteByIdAndOwner(id, user);
    collectionManager.removeById(id);
    if (removedFromDb) {
      return CommandResponse.success(String.format("Элемент с id=%d удалён", id));
    }
    return CommandResponse.success(String.format("Элемент с id=%d уже удалён", id));
  }

  private static int resolveId(CommandRequest request) throws CommandExecutionException {
    IdPayload idPayload =
        CommandPayloads.requireIdPayload(
            request,
            () -> new CommandExecutionException("Для remove_by_id требуется payload с полем id"));
    return idPayload.getId();
  }
}
