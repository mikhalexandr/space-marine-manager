package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.managers.CollectionManager;
import java.util.Comparator;
import java.util.List;

public class ShowCommand extends Command {
  private final CollectionManager collectionManager;

  public ShowCommand(CollectionManager collectionManager) {
    super("show", "вывести все элементы коллекции");
    this.collectionManager = collectionManager;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    List<SpaceMarine> sortedCollection =
        collectionManager.snapshot().stream()
            .sorted(Comparator.comparing(SpaceMarine::getId))
            .toList();
    if (sortedCollection.isEmpty()) {
      return CommandResponse.success("Коллекция пуста", sortedCollection);
    }
    return CommandResponse.success("Элементы коллекции:", sortedCollection);
  }
}
