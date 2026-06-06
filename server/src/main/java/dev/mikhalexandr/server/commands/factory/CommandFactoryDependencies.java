package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;

public record CommandFactoryDependencies(
    CommandManager commandManager,
    CollectionManager collectionManager,
    SpaceMarineRepository spaceMarineRepository) {}
