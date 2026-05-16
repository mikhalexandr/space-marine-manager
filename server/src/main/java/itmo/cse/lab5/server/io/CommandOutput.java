package itmo.cse.lab5.server.io;

import itmo.cse.lab5.common.dto.response.CommandResponse;
import itmo.cse.lab5.common.util.Validator;

/**
 * Отвечает за вывод результатов выполнения команд.
 */
public class CommandOutput {
    /**
     * Печатает приглашение командной строки.
     */
    public void printPrompt() {
        System.out.print("$ ");
    }

    /**
     * Печатает стандартное сообщение.
     *
     * @param message текст сообщения
     */
    public void printMessage(String message) {
        if (Validator.isValidString(message)) {
            System.out.println(message);
        }
    }

    /**
     * Печатает сообщение об ошибке.
     *
     * @param message текст ошибки
     */
    public void printError(String message) {
        if (Validator.isValidString(message)) {
            System.err.println(message);
        }
    }

    /**
     * Печатает ответ выполнения команды.
     *
     * @param response DTO ответа
     */
    public void printResponse(CommandResponse response) {
        if (response == null) {
            printError("Ошибка: пустой ответ от команды");
            return;
        }

        if (response.isSuccess()) {
            printMessage(response.getMessage());
        } else {
            printError(response.getMessage());
        }
    }
}
