package dev.mikhalexandr.client.network;

import dev.mikhalexandr.client.exceptions.RequestDeadlineExceededException;
import dev.mikhalexandr.client.security.TrustAnchor;
import dev.mikhalexandr.common.dto.auth.UserCredentials;
import dev.mikhalexandr.common.dto.event.CollectionEvent;
import dev.mikhalexandr.common.dto.event.ServerMessage;
import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.security.crypto.KeyAgreementService;
import dev.mikhalexandr.common.security.crypto.MessageSigner;
import dev.mikhalexandr.common.security.crypto.SessionCipher;
import dev.mikhalexandr.common.security.crypto.SessionKeys;
import dev.mikhalexandr.common.security.handshake.ClientHello;
import dev.mikhalexandr.common.security.handshake.HandshakeMessage;
import dev.mikhalexandr.common.security.handshake.ServerHello;
import dev.mikhalexandr.common.util.Bytes;
import dev.mikhalexandr.common.util.Serializer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpClient implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(TcpClient.class);
  private static final long RETRY_BACKOFF_STEP_MILLIS = 300L;
  private static final int SELECT_TIMEOUT_MILLIS = 200;

  private final String host;
  private final int port;
  private final int connectMaxAttempts;
  private final int deadlineMaxAttempts;
  private final long connectTimeoutMillis;
  private final long requestDeadlineMillis;
  private final TrustAnchor trustAnchor;
  private final String clientId = UUID.randomUUID().toString();

  private final Object connectionLock = new Object();
  private final Object writeLock = new Object();
  private final Map<String, CompletableFuture<CommandResponse>> pending = new ConcurrentHashMap<>();

  private Selector selector;
  private SocketChannel channel;
  private SelectionKey channelKey;
  private InputStream in;
  private volatile OutputStream out;
  private X509Certificate serverCertificate;
  private volatile SessionCipher sessionCipher;
  private UserCredentials credentials;
  private Thread readerThread;
  private volatile boolean shutdown;
  private volatile Consumer<CollectionEvent> eventListener;

  public TcpClient(
      String host,
      int port,
      int connectMaxAttempts,
      int deadlineMaxAttempts,
      long connectTimeoutMillis,
      long requestDeadlineMillis,
      TrustAnchor trustAnchor) {
    this.host = host;
    this.port = port;
    this.connectMaxAttempts = connectMaxAttempts;
    this.deadlineMaxAttempts = deadlineMaxAttempts;
    this.connectTimeoutMillis = connectTimeoutMillis;
    this.requestDeadlineMillis = requestDeadlineMillis;
    this.trustAnchor = trustAnchor;
  }

  public CommandResponse send(CommandRequest request) throws IOException {
    attachCredentials(request);
    int connectAttempts = 0;
    int deadlineAttempts = 0;
    while (true) {
      try {
        ensureConnected();
        return exchange(request);
      } catch (RequestDeadlineExceededException e) {
        if (++deadlineAttempts >= deadlineMaxAttempts) {
          throw e;
        }
        logDeadlineRetry(deadlineAttempts, request);
      } catch (ConnectException e) {
        closeQuietly();
        throw connectionRefused(e);
      } catch (IOException e) {
        closeQuietly();
        if (++connectAttempts >= connectMaxAttempts) {
          throw unavailable(e);
        }
        reconnectAfter(connectAttempts, e);
      }
    }
  }

  private void logDeadlineRetry(int attempt, CommandRequest request) {
    LOGGER.warn(
        "Дедлайн ответа (попытка {} из {}), повтор тем же requestId {}",
        attempt,
        deadlineMaxAttempts,
        request.getRequestId());
  }

  private void reconnectAfter(int attempt, IOException cause) {
    LOGGER.warn(
        "Соединение оборвалось (попытка {} из {}: {}), переподключаюсь...",
        attempt,
        connectMaxAttempts,
        cause.getMessage());
    sleepQuietly(attempt * RETRY_BACKOFF_STEP_MILLIS);
  }

  private IOException connectionRefused(ConnectException cause) {
    return new IOException(
        String.format("Сервер недоступен (%s:%d): %s", host, port, cause.getMessage()), cause);
  }

  private IOException unavailable(IOException cause) {
    return new IOException(
        String.format(
            "Сервер временно недоступен (%s:%d), попыток подключения: %d",
            host, port, connectMaxAttempts),
        cause);
  }

  public void setEventListener(Consumer<CollectionEvent> listener) {
    this.eventListener = listener;
  }

  @Override
  public void close() {
    shutdown = true;
    closeQuietly();
  }

  public void setCredentials(UserCredentials credentials) {
    this.credentials = credentials;
  }

  private void attachCredentials(CommandRequest request) {
    if (request.getCredentials() == null && credentials != null) {
      request.setCredentials(credentials);
    }
  }

  private CommandResponse exchange(CommandRequest request) throws IOException {
    String requestId = request.getRequestId();
    CompletableFuture<CommandResponse> future = new CompletableFuture<>();
    pending.put(requestId, future);
    try {
      writeRequest(request);
      return future.get(requestDeadlineMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new RequestDeadlineExceededException(
          "Сервер не ответил за "
              + requestDeadlineMillis
              + " мс. Запрос мог быть применён; повтор безопасен за счёт идемпотентности");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException io) {
        throw io;
      }
      throw new IOException("Ошибка обмена с сервером", cause);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Ожидание ответа прервано", e);
    } finally {
      pending.remove(requestId, future);
    }
  }

  private void writeRequest(CommandRequest request) throws IOException {
    synchronized (writeLock) {
      SessionCipher cipher = this.sessionCipher;
      OutputStream output = this.out;
      if (cipher == null || output == null) {
        throw new IOException("Соединение не готово");
      }
      byte[] encrypted = cipher.encrypt(Serializer.serialize(request));
      FrameCodec.writeFrame(output, encrypted);
    }
  }

  private void ensureConnected() throws IOException {
    synchronized (connectionLock) {
      if (shutdown) {
        throw new IOException("Клиент закрыт");
      }
      if (channel != null
          && channel.isConnected()
          && sessionCipher != null
          && readerThread != null
          && readerThread.isAlive()) {
        return;
      }
      closeQuietly();
      LOGGER.info("Подключаюсь к серверу {}:{}", host, port);
      openConnection();
      switchToBlocking();
      performHandshake();
      startReader();
      LOGGER.info(
          "Handshake завершён: server CN={} (issuer={})",
          serverCertificate.getSubjectX500Principal(),
          serverCertificate.getIssuerX500Principal());
    }
  }

  private void openConnection() throws IOException {
    selector = Selector.open();
    channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(new InetSocketAddress(host, port));
    channelKey = channel.register(selector, SelectionKey.OP_CONNECT);

    long deadline = System.currentTimeMillis() + connectTimeoutMillis;
    while (!channel.isConnected()) {
      if (System.currentTimeMillis() > deadline) {
        throw new IOException("Таймаут подключения к серверу");
      }
      selector.select(SELECT_TIMEOUT_MILLIS);
      finishConnectIfReady();
    }
  }

  private void finishConnectIfReady() throws IOException {
    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
    while (iterator.hasNext()) {
      SelectionKey key = iterator.next();
      iterator.remove();
      if (key.isConnectable() && !channel.finishConnect()) {
        throw new IOException("Не удалось завершить установку соединения");
      }
    }
  }

  private void switchToBlocking() throws IOException {
    if (channelKey != null) {
      channelKey.cancel();
      channelKey = null;
    }
    if (selector != null) {
      selector.selectNow();
      selector.close();
      selector = null;
    }
    channel.configureBlocking(true);
    this.in = new BufferedInputStream(Channels.newInputStream(channel));
    this.out = new BufferedOutputStream(Channels.newOutputStream(channel));
  }

  private void performHandshake() throws IOException {
    KeyPair clientEphemeral = KeyAgreementService.generateEphemeralKeyPair();
    byte[] clientEphemeralEncoded =
        KeyAgreementService.encodePublicKey(clientEphemeral.getPublic());
    sendClientHello(clientEphemeralEncoded);

    ServerHello serverHello = receiveServerHello();
    serverCertificate = trustAnchor.verifyServerCertificate(serverHello.certificateDer(), host);
    verifyServerSignature(clientEphemeralEncoded, serverHello);

    PublicKey serverEphemeral =
        KeyAgreementService.decodePublicKey(serverHello.ephemeralPublicKey());
    byte[] sharedSecret =
        KeyAgreementService.computeSharedSecret(clientEphemeral.getPrivate(), serverEphemeral);
    byte[] transcriptHash =
        KeyAgreementService.transcriptHash(
            clientEphemeralEncoded, serverHello.ephemeralPublicKey());
    SessionKeys keys = KeyAgreementService.deriveSessionKeys(sharedSecret, transcriptHash);
    sessionCipher = new SessionCipher(keys.clientToServer(), keys.serverToClient());
  }

  private void sendClientHello(byte[] ephemeralPublicKey) throws IOException {
    ClientHello hello = new ClientHello(clientId, System.currentTimeMillis(), ephemeralPublicKey);
    FrameCodec.writeFrame(out, Serializer.serialize(hello));
  }

  private ServerHello receiveServerHello() throws IOException {
    HandshakeMessage message = deserializeHandshake(FrameCodec.readFrame(in));
    return message.asServerHello();
  }

  private void verifyServerSignature(byte[] clientEphemeralEncoded, ServerHello serverHello)
      throws IOException {
    byte[] transcript = Bytes.concat(clientEphemeralEncoded, serverHello.ephemeralPublicKey());
    MessageSigner.verify(transcript, serverHello.signature(), serverCertificate.getPublicKey());
  }

  private void startReader() {
    final SessionCipher cipher = this.sessionCipher;
    final InputStream input = this.in;
    Thread thread = new Thread(() -> readLoop(cipher, input), "tcp-client-reader");
    thread.setDaemon(true);
    readerThread = thread;
    thread.start();
  }

  private void readLoop(SessionCipher cipher, InputStream input) {
    try {
      while (true) {
        byte[] frame = FrameCodec.readFrame(input);
        ServerMessage message = deserializeServerMessage(cipher.decrypt(frame));
        dispatch(message);
      }
    } catch (IOException e) {
      failPending(e);
      LOGGER.debug("Поток-читатель завершён: {}", e.getMessage());
    }
  }

  private void dispatch(ServerMessage message) {
    if (message.getKind() == ServerMessage.Kind.EVENT) {
      Consumer<CollectionEvent> listener = eventListener;
      CollectionEvent event = message.getEvent();
      if (listener != null && event != null) {
        try {
          listener.accept(event);
        } catch (RuntimeException e) {
          LOGGER.warn("Ошибка обработки серверного события", e);
        }
      }
      return;
    }
    String correlationId = message.getCorrelationId();
    CompletableFuture<CommandResponse> future =
        correlationId == null ? null : pending.remove(correlationId);
    if (future != null) {
      future.complete(message.getResponse());
    } else {
      LOGGER.debug("Ответ без ожидающего запроса (correlationId={})", correlationId);
    }
  }

  private void failPending(IOException error) {
    pending.forEach((id, future) -> future.completeExceptionally(error));
    pending.clear();
  }

  private static ServerMessage deserializeServerMessage(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, ServerMessage.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать сообщение сервера", e);
    }
  }

  private static HandshakeMessage deserializeHandshake(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, HandshakeMessage.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать handshake-фрейм", e);
    }
  }

  private void closeQuietly() {
    synchronized (connectionLock) {
      serverCertificate = null;
      sessionCipher = null;
      channelKey = null;
      closeResource(in);
      in = null;
      closeResource(out);
      out = null;
      closeResource(channel);
      channel = null;
      closeResource(selector);
      selector = null;
    }
    failPending(new IOException("Соединение закрыто"));
  }

  private static void closeResource(Closeable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (IOException e) {
        LOGGER.debug("Не удалось закрыть ресурс клиента", e);
      }
    }
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
