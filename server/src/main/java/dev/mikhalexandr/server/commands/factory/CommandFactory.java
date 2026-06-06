package dev.mikhalexandr.server.commands.factory;

import dev.mikhalexandr.server.commands.CommandContract;
import java.util.List;

public interface CommandFactory {
  List<CommandContract> createAll();
}
