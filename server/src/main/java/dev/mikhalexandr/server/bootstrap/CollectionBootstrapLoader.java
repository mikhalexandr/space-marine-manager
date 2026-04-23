package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.server.exceptions.FileReadException;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Загружает коллекцию на этапе запуска сервера. */
final class CollectionBootstrapLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(CollectionBootstrapLoader.class);

  void load(CollectionManager collectionManager, FileManager fileManager) {
    try {
      collectionManager.setCollection(fileManager.load());
      String message = String.format("Загружено элементов: %d", collectionManager.size());
      LOGGER.info(message);
    } catch (FileReadException e) {
      String errorMessage = String.format("Ошибка загрузки: %s", e.getMessage());
      LOGGER.error(errorMessage, e);
    }
  }
}
