package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;

/**
 * Набор зависимостей, необходимых для создания команд в фабрике.
 *
 * @param commandManager менеджер команд
 * @param collectionManager менеджер коллекции в памяти
 * @param spaceMarineRepository репозиторий коллекции в БД
 */
public record CommandFactoryDependencies(
    CommandManager commandManager,
    CollectionManager collectionManager,
    SpaceMarineRepository spaceMarineRepository) {}
