package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.commands.CommandContract;
import java.util.List;

/** Фабрика создания команд. */
public interface CommandFactory {
  /** Создает все доступные команды. */
  List<CommandContract> createAll();
}
