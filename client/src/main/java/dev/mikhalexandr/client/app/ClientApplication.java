package dev.mikhalexandr.client.app;

import dev.mikhalexandr.client.cli.ClientSession;
import dev.mikhalexandr.client.commands.CommandRequestParser;
import dev.mikhalexandr.client.io.InputHandler;
import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.client.security.TrustAnchor;
import dev.mikhalexandr.common.util.Env;
import java.io.IOException;
import java.nio.file.Path;

/** Оркестратор клиентского приложения: инициализация и запуск сессии. */
public final class ClientApplication {
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 5050;
  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65535;
  private static final int MAX_RETRY_ATTEMPTS = 3;
  private static final long CONNECT_TIMEOUT_MILLIS = 2000;
  private static final long REQUEST_TIMEOUT_MILLIS = 5000;
  private static final String ENV_CA_CERT_PATH = "CA_CERT_PATH";
  private static final String DEFAULT_CA_CERT_PATH = "certs/ca.crt";

  /** Запускает клиент с переданными аргументами. */
  public void start(String[] args) {
    ClientEndpoint endpoint = parseEndpoint(args);
    TrustAnchor trustAnchor = loadTrustAnchor();
    if (trustAnchor == null) {
      return;
    }
    TcpClient tcpClient = buildTcpClient(endpoint, trustAnchor);
    if (tcpClient == null) {
      return;
    }

    printWelcome(endpoint);
    ClientSession session =
        new ClientSession(new CommandRequestParser(), new InputHandler(), tcpClient);
    session.run();
  }

  private static TrustAnchor loadTrustAnchor() {
    String caCertPath = Env.orDefault(ENV_CA_CERT_PATH, DEFAULT_CA_CERT_PATH);
    try {
      return TrustAnchor.loadFromFile(Path.of(caCertPath));
    } catch (IOException e) {
      System.err.println("Не удалось загрузить " + caCertPath);
      return null;
    }
  }

  private static TcpClient buildTcpClient(ClientEndpoint endpoint, TrustAnchor trustAnchor) {
    try {
      return new TcpClient(
          endpoint.host(),
          endpoint.port(),
          MAX_RETRY_ATTEMPTS,
          CONNECT_TIMEOUT_MILLIS,
          REQUEST_TIMEOUT_MILLIS,
          trustAnchor);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка запуска клиента: " + e.getMessage());
      return null;
    }
  }

  private static ClientEndpoint parseEndpoint(String[] args) {
    if (args.length == 0) {
      return new ClientEndpoint(DEFAULT_HOST, DEFAULT_PORT);
    }
    if (args.length == 1) {
      if (isPortArgument(args[0])) {
        return new ClientEndpoint(DEFAULT_HOST, parsePort(args[0]));
      }
      return new ClientEndpoint(args[0], DEFAULT_PORT);
    }
    return new ClientEndpoint(args[0], parsePort(args[1]));
  }

  private static boolean isPortArgument(String arg) {
    try {
      Integer.parseInt(arg);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static void printWelcome(ClientEndpoint endpoint) {
    System.out.printf("Клиент запущен. Сервер: %s:%d%n", endpoint.host(), endpoint.port());
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

  private record ClientEndpoint(String host, int port) {}
}
