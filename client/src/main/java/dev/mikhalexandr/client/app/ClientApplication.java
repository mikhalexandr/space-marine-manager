package dev.mikhalexandr.client.app;

import dev.mikhalexandr.client.cli.ClientSession;
import dev.mikhalexandr.client.commands.CommandRequestParser;
import dev.mikhalexandr.client.io.InputHandler;
import dev.mikhalexandr.client.network.TcpClient;

/** Оркестратор клиентского приложения: инициализация и запуск сессии. */
public final class ClientApplication {
  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final int DEFAULT_PORT = 5050;
  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65535;
  private static final int MAX_RETRY_ATTEMPTS = 3;
  private static final long CONNECT_TIMEOUT_MILLIS = 2000;
  private static final long REQUEST_TIMEOUT_MILLIS = 5000;

  /** Запускает клиент с переданными аргументами. */
  public void start(String[] args) {
    TcpClient tcpClient = buildTcpClient(args);
    if (tcpClient == null) {
      return;
    }

    printWelcome(args);
    ClientSession session =
        new ClientSession(new CommandRequestParser(), new InputHandler(), tcpClient);
    session.run();
  }

  private static TcpClient buildTcpClient(String[] args) {
    String host = args.length > 0 ? args[0] : DEFAULT_HOST;

    try {
      int port = args.length > 1 ? parsePort(args[1]) : DEFAULT_PORT;
      return new TcpClient(
          host, port, MAX_RETRY_ATTEMPTS, CONNECT_TIMEOUT_MILLIS, REQUEST_TIMEOUT_MILLIS);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка запуска клиента: " + e.getMessage());
      return null;
    }
  }

  private static void printWelcome(String[] args) {
    String host = args.length > 0 ? args[0] : DEFAULT_HOST;
    int port = args.length > 1 ? parsePort(args[1]) : DEFAULT_PORT;
    System.out.printf("Клиент запущен. Сервер: %s:%d%n", host, port);
    System.out.println("Введите команду (exit для выхода):");
  }

  private static int parsePort(String portArg) {
    try {
      int port = Integer.parseInt(portArg);
      if (port < MIN_PORT || port > MAX_PORT) {
        throw new IllegalArgumentException(
            String.format("Порт должен быть в диапазоне %d..%d", MIN_PORT, MAX_PORT));
      }
      return port;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Порт клиента должен быть целым числом", e);
    }
  }
}
