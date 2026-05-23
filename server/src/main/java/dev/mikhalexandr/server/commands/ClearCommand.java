package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;

/** Команда {@code clear}: удаляет из коллекции элементы текущего пользователя */
public class ClearCommand extends Command {
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  /**
   * @param collectionManager менеджер коллекции в памяти
   * @param repository репозиторий коллекции в БД
   */
  public ClearCommand(CollectionManager collectionManager, SpaceMarineRepository repository) {
    super("clear", "очистить свои элементы коллекции");
    this.collectionManager = collectionManager;
    this.repository = repository;
  }

  /**
   * Удаляет из БД и памяти все элементы, принадлежащие текущему пользователю
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   */
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
