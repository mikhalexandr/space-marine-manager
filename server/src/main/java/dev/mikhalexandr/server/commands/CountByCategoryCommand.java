package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.payload.CategoryPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code count_by_category}: выводит количество элементов заданной категории. */
public class CountByCategoryCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public CountByCategoryCommand(CollectionManager collectionManager) {
    super("count_by_category", "<category>", "вывести количество элементов с заданной категорией");
    this.collectionManager = collectionManager;
  }

  /**
   * Подсчитывает элементы с указанной категорией.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если категория не передана или некорректна
   */
  @Override
  public CommandResponse execute(CommandRequest request) throws CommandExecutionException {
    AstartesCategory category = resolveCategory(request);
    long count = collectionManager.countByCategory(category);
    return CommandResponse.success(
        String.format("Количество элементов с категорией %s:%d", category, count));
  }

  private static AstartesCategory resolveCategory(CommandRequest request)
      throws CommandExecutionException {
    if (request == null || !(request.getPayload() instanceof CategoryPayload payload)) {
      throw new CommandExecutionException(
          "Для count_by_category требуется payload с полем category");
    }
    if (payload.getCategory() == null) {
      throw new CommandExecutionException(
          "Для count_by_category payload.category не может быть null");
    }
    return payload.getCategory();
  }
}
