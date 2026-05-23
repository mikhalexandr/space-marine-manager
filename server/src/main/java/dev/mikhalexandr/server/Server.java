package dev.mikhalexandr.server;

import dev.mikhalexandr.common.util.Validator;
import dev.mikhalexandr.server.bootstrap.ServerBootstrap;

/** Точка входа серверного приложения */
public final class Server {
  private static final int DEFAULT_PORT = 5050;
  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65535;

  private Server() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * Запускает однопоточный приём подключений и многопоточную обработку запросов
   *
   * @param args порт, если надо
   */
  public static void main(String[] args) {
    int port;
    try {
      port = parsePort(args);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка инициализации сервера: " + e.getMessage());
      System.exit(1);
      return;
    }

    new ServerBootstrap().run(port);
  }

  private static int parsePort(String[] args) {
    if (args.length == 0 || !Validator.isValidString(args[0])) {
      return DEFAULT_PORT;
    }

    try {
      int port = Integer.parseInt(args[0].trim());
      if (port < MIN_PORT || port > MAX_PORT) {
        throw new IllegalArgumentException(
            String.format("Порт должен быть в диапазоне %d..%d", MIN_PORT, MAX_PORT));
      }
      return port;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Порт должен быть целым числом", e);
    }
  }
}
