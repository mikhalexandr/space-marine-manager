package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;

public class ClearCommand extends Command {
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  public ClearCommand(CollectionManager collectionManager, SpaceMarineRepository repository) {
    super("clear", "очистить свои элементы коллекции");
    this.collectionManager = collectionManager;
    this.repository = repository;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    String user = currentUser(request);
    int removed = repository.deleteByOwner(user);
    collectionManager.removeByOwner(user);
    if (removed == 0) {
      return CommandResponse.success("У вас нет элементов для удаления");
    }
    return CommandResponse.success(String.format("Удалено ваших элементов: %d", removed));
  }
}
