package dev.mikhalexandr.server.commands;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.exceptions.CommandExecutionException;

/** Контракт серверной команды. */
public interface CommandContract {
  /**
   * @return имя команды
   */
  String getName();

  /**
   * @return строка с описанием аргументов команды
   */
  String getArgs();

  /**
   * @return описание команды
   */
  String getDescription();

  /**
   * Выполняет команду и формирует DTO-ответ для клиент-серверного обмена.
   *
   * @param request DTO-запрос команды
   * @return DTO-ответ выполнения
   * @throws CommandExecutionException если выполнить команду не удалось
   */
  CommandResponse execute(CommandRequest request) throws CommandExecutionException;
}
