package dev.mikhalexandr.server.auth;

import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.server.db.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;

  public AuthService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public CommandResponse register(UserCredentials credentials) {
    String validationError = validate(credentials);
    if (validationError != null) {
      return CommandResponse.error(validationError);
    }
    String login = credentials.login().trim();
    if (userRepository.exists(login)) {
      return CommandResponse.error("Пользователь '" + login + "' уже зарегистрирован");
    }
    userRepository.create(login, PasswordHasher.hash(credentials.password()));
    LOGGER.info("Зарегистрирован новый пользователь: {}", login);
    return CommandResponse.success("Регистрация успешна. Добро пожаловать, " + login + "!");
  }

  public CommandResponse login(UserCredentials credentials) {
    if (isAuthenticated(credentials)) {
      return CommandResponse.success(
          "Вход выполнен. Здравствуйте, " + credentials.login().trim() + "!");
    }
    return CommandResponse.error("Неверный логин или пароль");
  }

  public boolean isAuthenticated(UserCredentials credentials) {
    if (validate(credentials) != null) {
      return false;
    }
    Optional<String> storedHash = userRepository.findPasswordHash(credentials.login().trim());
    return storedHash
        .map(hash -> hash.equals(PasswordHasher.hash(credentials.password())))
        .orElse(false);
  }

  private static String validate(UserCredentials credentials) {
    if (credentials == null) {
      return "Не переданы учётные данные пользователя";
    }
    if (credentials.login() == null || credentials.login().isBlank()) {
      return "Логин не может быть пустым";
    }
    if (credentials.password() == null || credentials.password().isEmpty()) {
      return "Пароль не может быть пустым";
    }
    return null;
  }
}
