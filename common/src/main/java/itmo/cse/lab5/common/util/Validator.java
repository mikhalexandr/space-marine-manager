package itmo.cse.lab5.common.util;

import itmo.cse.lab5.common.exceptions.ValidationException;

/**
 * Набор методов валидации доменных данных.
 */
public final class Validator {
    private Validator() {
        throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
    }

    /**
     * Проверяет, что строка не null и не пустая.
     *
     * @param str входная строка
     * @return true, если строка валидна
     */
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Проверяет, что значение строго больше заданного минимума.
     *
     * @param value значение для проверки
     * @param min нижняя граница
     * @return true, если value > min
     */
    public static boolean isGreaterThan(double value, double min) {
        return value > min;
    }

    /**
     * Валидирует строковое поле.
     *
     * @param s значение поля
     * @param fieldName имя поля в сообщении об ошибке
     * @throws ValidationException если строка пустая или null
     */
    public static void validateString(String s, String fieldName) {
        if (s == null || s.trim().isEmpty()) {
            throw new ValidationException(
                    String.format("%s не может быть null или пустым", fieldName)
            );
        }
    }

    /**
     * Валидирует числовое поле, которое должно быть больше минимума.
     *
     * @param value значение поля
     * @param min нижняя граница
     * @param fieldName имя поля в сообщении об ошибке
     * @throws ValidationException если {@code value <= min}
     */
    public static void validateGreaterThan(double value, double min, String fieldName) {
        if (value <= min) {
            throw new ValidationException(
                    String.format("%s должен быть больше %s, получено: %s", fieldName, value, min)
            );
        }
    }

    /**
     * Валидирует, что значение не равно null.
     *
     * @param value значение поля
     * @param fieldName имя поля в сообщении об ошибке
     * @throws ValidationException если value == null
     */
    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(
                    String.format("%s не может быть null", fieldName)
            );
        }
    }

    /**
     * Валидирует идентификатор сущности.
     *
     * @param id идентификатор
     * @throws ValidationException если id null или {@code id <= 0}
     */
    public static void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new ValidationException(
                    String.format("id должен быть больше 0, получено: %s", id)
            );
        }
    }
}
