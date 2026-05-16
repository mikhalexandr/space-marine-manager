package itmo.cse.lab5.server.managers;

import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.dto.response.CommandResponse;

/**
 * Контракт для выполнения команд по DTO-запросу.
 */
public interface CommandExecutor {
    /**
     * Выполняет команду и возвращает DTO-ответ.
     *
     * @param request DTO запроса
     * @return DTO ответа
     */
    CommandResponse execute(CommandRequest request);
}
