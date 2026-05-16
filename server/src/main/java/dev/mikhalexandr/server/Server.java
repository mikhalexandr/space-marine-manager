package dev.mikhalexandr.server;

import dev.mikhalexandr.common.util.Validator;
import dev.mikhalexandr.server.bootstrap.ServerBootstrap;

/** Точка входа серверного приложения. */
public final class Server {
  private static final int DEFAULT_PORT = 5050;
  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65535;

  private Server() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * Запускает однопоточный TCP-сервер.
   *
   * @param args первый аргумент должен содержать путь к JSON-файлу коллекции, второй (опционально)
   *     TCP-порт
   */
  public static void main(String[] args) {
    if (args.length == 0 || !Validator.isValidString(args[0])) {
      System.err.printf(
          "Ошибка инициализации сервиса%nФормат запуска: java -jar [SERVER_JAR] <путь к файлу> [порт]%n");
      System.exit(1);
    }

    int port;
    try {
      port = parsePort(args);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка инициализации сервиса\n" + e.getMessage());
      System.exit(1);
      return;
    }

    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.run(args[0], port);
  }

  private static int parsePort(String[] args) {
    if (args.length < 2 || !Validator.isValidString(args[1])) {
      return DEFAULT_PORT;
    }

    try {
      int port = Integer.parseInt(args[1]);
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
