package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CollectionManager;

public class InfoCommand extends Command {
  private final CollectionManager collectionManager;

  public InfoCommand(CollectionManager collectionManager) {
    super("info", "вывести информацию о коллекции");
    this.collectionManager = collectionManager;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    return CommandResponse.success(
        String.format(
            "Тип коллекции: %s%nДата инициализации: %s%nКоличество элементов: %d",
            collectionManager.getType(),
            collectionManager.getInitializationDateFormatted(),
            collectionManager.size()));
  }
}
