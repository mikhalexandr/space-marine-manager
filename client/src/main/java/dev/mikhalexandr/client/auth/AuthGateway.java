package dev.mikhalexandr.client.auth;

import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.request.payload.NoArgsPayload;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import java.io.IOException;
import java.util.Scanner;

/**
 * После успешной аутентификации учётные данные сохраняются в {@link TcpClient} и прикрепляются к
 * запросам потом
 */
public final class AuthGateway {
  private final TcpClient tcpClient;
  private final Scanner scanner;

  /**
   * @param tcpClient транспорт для отправки запросов на сервер
   * @param scanner источник ввода клиента
   */
  public AuthGateway(TcpClient tcpClient, Scanner scanner) {
    this.tcpClient = tcpClient;
    this.scanner = scanner;
  }

  /**
   * Запрашивает действие, пока пользователь не войдёт, не зарегистрируется или не выйдет
   *
   * @return true, если аутентификация прошла успешно
   */
  public boolean authenticate() {
    System.out.println("Space Marine Manager is here!");
    while (true) {
      String choice = readLine("Выберите: [1] вход  [2] регистрация  [exit] выход: ");
      if (choice == null || "exit".equalsIgnoreCase(choice.trim())) {
        return false;
      }
      CommandType type = resolveType(choice.trim());
      if (type == null) {
        System.out.println("| Неверный выбор. Введите 1, 2 или exit.");
      } else if (attempt(type)) {
        return true;
      }
    }
  }

  private boolean attempt(CommandType type) {
    String login = readLine("| Логин: ");
    String password = readLine("| Пароль: ");
    if (login == null || password == null) {
      return false;
    }
    UserCredentials credentials = new UserCredentials(login.trim(), password);
    CommandRequest request = new CommandRequest(type, NoArgsPayload.INSTANCE);
    request.setCredentials(credentials);
    try {
      return handleResponse(tcpClient.send(request), credentials);
    } catch (IOException e) {
      System.out.println("| Сервер недоступен: " + e.getMessage());
      return false;
    }
  }

  private boolean handleResponse(CommandResponse response, UserCredentials credentials) {
    String message = response.getMessage();
    if (message != null && !message.isBlank()) {
      System.out.println("| " + message);
    }
    if (response.isSuccess()) {
      tcpClient.setCredentials(credentials);
      return true;
    }
    return false;
  }

  private String readLine(String prompt) {
    System.out.print(prompt);
    if (!scanner.hasNextLine()) {
      return null;
    }
    return scanner.nextLine();
  }

  private static CommandType resolveType(String choice) {
    if ("1".equals(choice) || "login".equalsIgnoreCase(choice)) {
      return CommandType.LOGIN;
    }
    if ("2".equals(choice) || "register".equalsIgnoreCase(choice)) {
      return CommandType.REGISTER;
    }
    return null;
  }
}
