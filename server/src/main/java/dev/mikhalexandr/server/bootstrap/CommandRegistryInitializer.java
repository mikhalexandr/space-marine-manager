package dev.mikhalexandr.server.bootstrap;

import dev.mikhalexandr.server.commands.CommandContract;
import dev.mikhalexandr.server.commands.factory.CommandFactory;
import dev.mikhalexandr.server.commands.factory.CommandFactoryDependencies;
import dev.mikhalexandr.server.commands.factory.DefaultCommandFactory;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;

/** Отвечает за регистрацию команд сервера через фабрику. */
final class CommandRegistryInitializer {
  void register(CommandManager commandManager, CollectionManager collectionManager) {
    CommandFactory commandFactory =
        new DefaultCommandFactory(
            new CommandFactoryDependencies(commandManager, collectionManager));

    for (CommandContract command : commandFactory.createAll()) {
      commandManager.register(command);
    }
  }
}
