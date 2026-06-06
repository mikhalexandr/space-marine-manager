package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.server.commands.CommandContract;
import dev.mikhalexandr.server.commands.factory.CommandFactory;
import dev.mikhalexandr.server.commands.factory.CommandFactoryDependencies;
import dev.mikhalexandr.server.commands.factory.DefaultCommandFactory;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;

final class CommandRegistryInitializer {
  void register(
      CommandManager commandManager,
      CollectionManager collectionManager,
      SpaceMarineRepository repository) {
    CommandFactory commandFactory =
        new DefaultCommandFactory(
            new CommandFactoryDependencies(commandManager, collectionManager, repository));

    for (CommandContract command : commandFactory.createAll()) {
      commandManager.register(command);
    }
  }
}
