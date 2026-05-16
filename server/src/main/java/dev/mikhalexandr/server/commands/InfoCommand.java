package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code info}: выводит информацию о текущей коллекции. */
public class InfoCommand extends Command {
  private final CollectionManager collectionManager;

  /**
   * @param collectionManager менеджер коллекции
   */
  public InfoCommand(CollectionManager collectionManager) {
    super("info", "вывести информацию о коллекции");
    this.collectionManager = collectionManager;
  }

  /**
   * Печатает тип коллекции, дату инициализации и количество элементов.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   */
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
