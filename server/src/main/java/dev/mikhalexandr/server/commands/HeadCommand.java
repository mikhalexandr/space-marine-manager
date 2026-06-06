package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.managers.CollectionManager;

public class HeadCommand extends Command {
  private final CollectionManager collectionManager;

  public HeadCommand(CollectionManager collectionManager) {
    super("head", "вывести первый элемент коллекции");
    this.collectionManager = collectionManager;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    SpaceMarine head = collectionManager.head();
    if (head == null) {
      return CommandResponse.success("Коллекция пуста");
    }
    return CommandResponse.success(head.toString());
  }
}
