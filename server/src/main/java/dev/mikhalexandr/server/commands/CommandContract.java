package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;

public interface CommandContract {
  String getName();

  String getArgs();

  String getDescription();

  CommandResponse execute(CommandRequest request) throws CommandExecutionException;
}
