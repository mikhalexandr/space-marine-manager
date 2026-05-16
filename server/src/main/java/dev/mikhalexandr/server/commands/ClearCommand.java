package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code clear}: очищает коллекцию. */
public class ClearCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public ClearCommand(CollectionManager collectionManager) {
    super("clear", "очистить коллекцию");
    this.collectionManager = collectionManager;
  }

  /**
   * Очищает коллекцию.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   */
  @Override
  public CommandResponse execute(CommandRequest request) {
    collectionManager.clear();
    return CommandResponse.success("Коллекция очищена.");
  }
}
