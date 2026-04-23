package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;

/** Базовый абстрактный тип команды. */
public abstract class Command implements CommandContract {
  private final String name;
  private final String arguments;
  private final String description;

  /**
   * Создает команду с явным описанием аргументов.
   *
   * @param name имя команды
   * @param arguments ожидаемые аргументы в текстовом виде
   * @param description описание назначения команды
   */
  public Command(String name, String arguments, String description) {
    this.name = name;
    this.arguments = arguments;
    this.description = description;
  }

  /**
   * Создает команду без обязательных аргументов.
   *
   * @param name имя команды
   * @param description описание назначения команды
   */
  public Command(String name, String description) {
    this(name, "", description);
  }

  /**
   * @return имя команды
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @return строка с описанием аргументов команды
   */
  @Override
  public String getArgs() {
    return arguments;
  }

  /**
   * @return описание команды
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public abstract CommandResponse execute(CommandRequest request) throws CommandExecutionException;
}
