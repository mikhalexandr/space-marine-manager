package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;

/** Набор зависимостей, необходимых для создания команд в фабрике. */
public record CommandFactoryDependencies(
    CommandManager commandManager, CollectionManager collectionManager) {}
