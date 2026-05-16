package itmo.cse.lab5.common.dto.response;

import java.io.Serializable;

/**
 * Ответ на выполнение команды.
 */
public final class CommandResponse implements Serializable {
    private final boolean success;
    private final String message;
    private final boolean exitRequested;

    /**
     * @param success признак успешного выполнения
     * @param message текст ответа
     * @param exitRequested признак необходимости завершения работы
     */
    public CommandResponse(boolean success, String message, boolean exitRequested) {
        this.success = success;
        this.message = message;
        this.exitRequested = exitRequested;
    }

    /**
     * @param message текст ответа
     * @param exitRequested признак необходимости завершения работы
     * @return успешный ответ
     */
    public static CommandResponse success(String message, boolean exitRequested) {
        return new CommandResponse(true, message, exitRequested);
    }

    /**
     * @param message текст ошибки
     * @return ответ с ошибкой
     */
    public static CommandResponse failure(String message) {
        return new CommandResponse(false, message,
                false);
    }

    /**
     * @return true, если команда выполнена успешно
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return текст ответа
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return true, если нужно завершить приложение
     */
    public boolean isExitRequested() {
        return exitRequested;
    }
}
