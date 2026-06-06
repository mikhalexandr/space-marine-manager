package dev.mikhalexandr.client.app;

import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.client.security.TrustAnchor;
import dev.mikhalexandr.common.util.Env;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public record ClientConfig(
    String host,
    int port,
    String caCertPath,
    long connectTimeoutMillis,
    long requestDeadlineMillis,
    int connectMaxAttempts,
    int deadlineMaxAttempts) {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 5050;
  private static final String ENV_HOST = "SERVER_HOST";
  private static final String ENV_CA_CERT_PATH = "CA_CERT_PATH";
  private static final String DEFAULT_CA_CERT_PATH = "client/certs/ca.crt";
  private static final String ENV_CONNECT_TIMEOUT_MILLIS = "CLIENT_CONNECT_TIMEOUT_MILLIS";
  private static final String ENV_REQUEST_DEADLINE_MILLIS = "CLIENT_REQUEST_DEADLINE_MILLIS";
  private static final String ENV_CONNECT_MAX_ATTEMPTS = "CLIENT_CONNECT_MAX_ATTEMPTS";
  private static final String ENV_DEADLINE_MAX_ATTEMPTS = "CLIENT_DEADLINE_MAX_ATTEMPTS";
  private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 2000L;
  private static final long DEFAULT_REQUEST_DEADLINE_MILLIS = 30000L;
  private static final int DEFAULT_CONNECT_MAX_ATTEMPTS = 3;
  private static final int DEFAULT_DEADLINE_MAX_ATTEMPTS = 2;

  public static ClientConfig fromEnvAndArgs(List<String> args) {
    String host = args.isEmpty() ? Env.orDefault(ENV_HOST, DEFAULT_HOST) : args.get(0);
    int port = args.size() > 1 ? Integer.parseInt(args.get(1)) : DEFAULT_PORT;
    return new ClientConfig(
        host,
        port,
        Env.orDefault(ENV_CA_CERT_PATH, DEFAULT_CA_CERT_PATH),
        Env.longOrDefault(ENV_CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT_MILLIS),
        Env.longOrDefault(ENV_REQUEST_DEADLINE_MILLIS, DEFAULT_REQUEST_DEADLINE_MILLIS),
        (int) Env.longOrDefault(ENV_CONNECT_MAX_ATTEMPTS, DEFAULT_CONNECT_MAX_ATTEMPTS),
        (int) Env.longOrDefault(ENV_DEADLINE_MAX_ATTEMPTS, DEFAULT_DEADLINE_MAX_ATTEMPTS));
  }

  public TcpClient newClient() throws IOException {
    TrustAnchor trustAnchor = TrustAnchor.loadFromFile(Path.of(caCertPath));
    return new TcpClient(
        host,
        port,
        connectMaxAttempts,
        deadlineMaxAttempts,
        connectTimeoutMillis,
        requestDeadlineMillis,
        trustAnchor);
  }
}
