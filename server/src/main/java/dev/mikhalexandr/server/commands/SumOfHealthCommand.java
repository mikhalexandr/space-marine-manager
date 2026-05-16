package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code sum_of_health}: выводит сумму поля health по коллекции. */
public class SumOfHealthCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public SumOfHealthCommand(CollectionManager collectionManager) {
    super("sum_of_health", "вывести сумму здоровья всех элементов");
    this.collectionManager = collectionManager;
  }

  /**
   * Печатает сумму здоровья всех элементов.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   */
  @Override
  public CommandResponse execute(CommandRequest request) {
    return CommandResponse.success(
        String.format("Сумма здоровья: %f", collectionManager.sumOfHealth()));
  }
}
