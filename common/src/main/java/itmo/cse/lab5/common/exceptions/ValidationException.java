package itmo.cse.lab5.common.exceptions;

/**
 * Ошибка валидации модели.
 */
public class ValidationException extends RuntimeException {
    /**
     * @param message текст ошибки
     */
    public ValidationException(String message) {
        super(message);
    }
}
