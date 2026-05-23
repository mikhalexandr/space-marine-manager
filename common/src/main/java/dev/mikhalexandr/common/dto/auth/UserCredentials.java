package dev.mikhalexandr.common.dto.auth;

import java.io.Serial;
import java.io.Serializable;

/**
 * Учётные данные пользователя
 *
 * @param login логин пользователя
 * @param password пароль
 */
public record UserCredentials(String login, String password) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
