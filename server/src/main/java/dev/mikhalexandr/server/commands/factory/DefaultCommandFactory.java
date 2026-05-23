package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.commands.AddCommand;
import dev.mikhalexandr.server.commands.AddIfMinCommand;
import dev.mikhalexandr.server.commands.ClearCommand;
import dev.mikhalexandr.server.commands.CommandContract;
import dev.mikhalexandr.server.commands.CountByCategoryCommand;
import dev.mikhalexandr.server.commands.HeadCommand;
import dev.mikhalexandr.server.commands.HelpCommand;
import dev.mikhalexandr.server.commands.HistoryCommand;
import dev.mikhalexandr.server.commands.InfoCommand;
import dev.mikhalexandr.server.commands.MaxByChapterCommand;
import dev.mikhalexandr.server.commands.RemoveByIdCommand;
import dev.mikhalexandr.server.commands.ShowCommand;
import dev.mikhalexandr.server.commands.SumOfHealthCommand;
import dev.mikhalexandr.server.commands.UpdateCommand;
import dev.mikhalexandr.server.db.SpaceMarineRepository;
import dev.mikhalexandr.server.managers.CollectionManager;
import dev.mikhalexandr.server.managers.CommandManager;
import java.util.ArrayList;
import java.util.List;

/** Стандартная фабрика команд сервера. */
public class DefaultCommandFactory implements CommandFactory {
  private final CommandManager commandManager;
  private final CollectionManager collectionManager;
  private final SpaceMarineRepository repository;

  /**
   * @param dependencies зависимости, необходимые для создания команд
   */
  public DefaultCommandFactory(CommandFactoryDependencies dependencies) {
    this.commandManager = dependencies.commandManager();
    this.collectionManager = dependencies.collectionManager();
    this.repository = dependencies.spaceMarineRepository();
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
    commands.add(new AddCommand(collectionManager, repository));
    commands.add(new UpdateCommand(collectionManager, repository));
    commands.add(new RemoveByIdCommand(collectionManager, repository));
    commands.add(new ClearCommand(collectionManager, repository));
    commands.add(new HeadCommand(collectionManager));
    commands.add(new AddIfMinCommand(collectionManager, repository));
    commands.add(new SumOfHealthCommand(collectionManager));
    commands.add(new MaxByChapterCommand(collectionManager));
    commands.add(new CountByCategoryCommand(collectionManager));
  }
}
