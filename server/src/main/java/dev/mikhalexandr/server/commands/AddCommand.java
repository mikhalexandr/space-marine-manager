package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.MarinePayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code add}: добавляет новый элемент в коллекцию. */
public class AddCommand extends Command {
  private static final int TEST = 5000;
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public AddCommand(CollectionManager collectionManager) {
    super("add", "{element}", "добавить новый элемент в коллекцию");
    this.collectionManager = collectionManager;
  }

  /**
   * Добавляет элемент в коллекцию (из payload или интерактивного ввода).
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если не удалось дочитать поля элемента
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    try {
      Thread.sleep(TEST);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    SpaceMarine spaceMarine = extractMarine(request);
    collectionManager.add(spaceMarine);
    return CommandResponse.success("Элемент успешно добавлен");
  }

  private static SpaceMarine extractMarine(CommandRequest request)
      throws CommandExecutionException {
    if (request == null || !(request.getPayload() instanceof MarinePayload payload)) {
      throw new CommandExecutionException("Для add требуется объектный payload SpaceMarine");
    }
    SpaceMarine spaceMarine = payload.getSpaceMarine();
    if (spaceMarine == null) {
      throw new CommandExecutionException("Для add требуется объект SpaceMarine");
    }
    return spaceMarine;
  }
}
