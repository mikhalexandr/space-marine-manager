package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CollectionManager;

public class SumOfHealthCommand extends Command {
  private final CollectionManager collectionManager;

  public SumOfHealthCommand(CollectionManager collectionManager) {
    super("sum_of_health", "вывести сумму здоровья всех элементов");
    this.collectionManager = collectionManager;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    return CommandResponse.success(
        String.format("Сумма здоровья: %f", collectionManager.sumOfHealth()));
  }
}
