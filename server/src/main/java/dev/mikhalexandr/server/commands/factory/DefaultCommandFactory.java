package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.commands.*;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;
import java.util.ArrayList;
import java.util.List;

/** Стандартная фабрика команд сервера. */
public class DefaultCommandFactory implements CommandFactory {
  private final CommandManager commandManager;
  private final CollectionManager collectionManager;

  /**
   * @param dependencies зависимости, необходимые для создания команд
   */
  public DefaultCommandFactory(CommandFactoryDependencies dependencies) {
    this.commandManager = dependencies.commandManager();
    this.collectionManager = dependencies.collectionManager();
  }

  @Override
  public List<CommandContract> createAll() {
    List<CommandContract> commands = new ArrayList<>();
    appendSystemCommands(commands);
    appendCollectionCommands(commands);
    return commands;
  }

  private void appendSystemCommands(List<CommandContract> commands) {
    commands.add(new HelpCommand(commandManager));
    commands.add(new HistoryCommand(commandManager));
  }

  private void appendCollectionCommands(List<CommandContract> commands) {
    commands.add(new InfoCommand(collectionManager));
    commands.add(new ShowCommand(collectionManager));
    commands.add(new AddCommand(collectionManager));
    commands.add(new UpdateCommand(collectionManager));
    commands.add(new RemoveByIdCommand(collectionManager));
    commands.add(new ClearCommand(collectionManager));
    commands.add(new HeadCommand(collectionManager));
    commands.add(new AddIfMinCommand(collectionManager));
    commands.add(new SumOfHealthCommand(collectionManager));
    commands.add(new MaxByChapterCommand(collectionManager));
    commands.add(new CountByCategoryCommand(collectionManager));
  }
}
