package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.server.managers.CollectionManager;

public class MaxByChapterCommand extends Command {
  private final CollectionManager collectionManager;

  public MaxByChapterCommand(CollectionManager collectionManager) {
    super("max_by_chapter", "вывести с максимальным значением chapter");
    this.collectionManager = collectionManager;
  }

  @Override
  public CommandResponse execute(CommandRequest request) {
    SpaceMarine max = collectionManager.maxByChapter();
    if (max == null) {
      return CommandResponse.success("Нет элементов с chapter");
    }
    return CommandResponse.success(max.toString());
  }
}
