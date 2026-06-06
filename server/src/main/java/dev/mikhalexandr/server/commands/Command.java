package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;

public abstract class Command implements CommandContract {
  private final String name;
  private final String arguments;
  private final String description;

  public Command(String name, String arguments, String description) {
    this.name = name;
    this.arguments = arguments;
    this.description = description;
  }

  public Command(String name, String description) {
    this(name, "", description);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getArgs() {
    return arguments;
  }

  @Override
  public String getDescription() {
    return description;
  }

  protected static String currentUser(CommandRequest request) {
    UserCredentials credentials = request.getCredentials();
    return credentials == null ? null : credentials.login().trim();
  }

  @Override
  public abstract CommandResponse execute(CommandRequest request) throws CommandExecutionException;
}
