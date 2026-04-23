package dev.mikhalexandr.server.managers;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;

/** Контракт для выполнения команд по DTO-запросу. */
public interface CommandExecutor {
  /**
   * Выполняет команду и возвращает DTO-ответ.
   *
   * @param request DTO запроса
   * @return DTO ответа
   */
  CommandResponse execute(CommandRequest request);
}
