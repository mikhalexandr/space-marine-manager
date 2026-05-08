package dev.mikhalexandr.client.network;

import dev.mikhalexandr.client.security.TrustAnchor;
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
import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Неблокирующий TCP-клиент с длинной сессией: одно соединение и один handshake на весь жизненный
 * цикл клиента
 */
public class TcpClient implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(TcpClient.class);
  private static final int MAX_RESPONSE_SIZE = 8 * 1024 * 1024;
  private static final long RETRY_BACKOFF_STEP_MILLIS = 300L;
  private static final int SELECT_TIMEOUT_MILLIS = 200;

  private final String host;
  private final int port;
  private final int maxAttempts;
  private final long connectTimeoutMillis;
  private final long requestTimeoutMillis;
  private final TrustAnchor trustAnchor;
  private final String clientId = UUID.randomUUID().toString();

  private Selector selector;
  private SocketChannel channel;
  private SelectionKey channelKey;
  private X509Certificate serverCertificate;
  private SessionCipher sessionCipher;

  public TcpClient(
      String host,
      int port,
      int maxAttempts,
      long connectTimeoutMillis,
      long requestTimeoutMillis,
      TrustAnchor trustAnchor) {
    this.host = host;
    this.port = port;
    this.maxAttempts = maxAttempts;
    this.connectTimeoutMillis = connectTimeoutMillis;
    this.requestTimeoutMillis = requestTimeoutMillis;
    this.trustAnchor = trustAnchor;
  }

  /**
   * Отправляет запрос на сервер. Лениво открывает соединение и делает handshake при первом вызове
   *
   * @param request запрос команды
   * @return ответ сервера
   */
  public CommandResponse send(CommandRequest request) throws IOException {
    IOException lastException = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        ensureConnected();
        return sendOnce(request);
      } catch (ConnectException e) {
        closeQuietly();
        throw new IOException(
            String.format("Сервер недоступен (%s:%d): %s", host, port, e.getMessage()), e);
      } catch (IOException e) {
        lastException = e;
        LOGGER.warn(
            "Попытка {} из {} оборвалась ({}), переподключаюсь...",
            attempt,
            maxAttempts,
            e.getMessage());
        closeQuietly();
        if (attempt < maxAttempts) {
          sleepQuietly(attempt * RETRY_BACKOFF_STEP_MILLIS);
        }
      }
    }
    throw new IOException(
        String.format("Сервер временно недоступен (%s:%d), попыток: %d", host, port, maxAttempts),
        lastException);
  }

  @Override
  public void close() {
    closeQuietly();
  }

  private void ensureConnected() throws IOException {
    if (channel != null && channel.isConnected() && sessionCipher != null) {
      return;
    }
    closeQuietly();
    LOGGER.info("Подключаюсь к серверу {}:{}", host, port);
    openConnection();
    performHandshake();
    LOGGER.info(
        "Handshake завершён: server CN={} (issuer={})",
        serverCertificate.getSubjectX500Principal(),
        serverCertificate.getIssuerX500Principal());
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
    channelKey.interestOps(0);
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
    writeFrame(FrameCodec.toFrame(Serializer.serialize(hello)));
  }

  private ServerHello receiveServerHello() throws IOException {
    HandshakeMessage message = deserializeHandshake(readFrame());
    return message.asServerHello();
  }

  private void verifyServerSignature(byte[] clientEphemeralEncoded, ServerHello serverHello)
      throws IOException {
    byte[] transcript = Bytes.concat(clientEphemeralEncoded, serverHello.ephemeralPublicKey());
    MessageSigner.verify(transcript, serverHello.signature(), serverCertificate.getPublicKey());
  }

  private CommandResponse sendOnce(CommandRequest request) throws IOException {
    byte[] plaintext = Serializer.serialize(request);
    byte[] encrypted = sessionCipher.encrypt(plaintext);
    writeFrame(FrameCodec.toFrame(encrypted));
    byte[] decrypted = sessionCipher.decrypt(readFrame());
    return deserializeResponse(decrypted);
  }

  private void writeFrame(byte[] frame) throws IOException {
    ByteBuffer buffer = ByteBuffer.wrap(frame);
    channelKey.interestOps(SelectionKey.OP_WRITE);
    long deadline = System.currentTimeMillis() + requestTimeoutMillis;
    while (buffer.hasRemaining()) {
      if (System.currentTimeMillis() > deadline) {
        throw new IOException("Таймаут отправки фрейма");
      }
      selector.select(SELECT_TIMEOUT_MILLIS);
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while (iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();
        if (key.isWritable()) {
          channel.write(buffer);
        }
      }
    }
    channelKey.interestOps(0);
  }

  private byte[] readFrame() throws IOException {
    channelKey.interestOps(SelectionKey.OP_READ);
    ReadState state = new ReadState();
    long deadline = System.currentTimeMillis() + requestTimeoutMillis;
    while (true) {
      if (System.currentTimeMillis() > deadline) {
        throw new IOException("Таймаут ожидания ответа от сервера");
      }
      selector.select(SELECT_TIMEOUT_MILLIS);
      byte[] payload = drainReadable(state);
      if (payload != null) {
        channelKey.interestOps(0);
        return payload;
      }
    }
  }

  private byte[] drainReadable(ReadState state) throws IOException {
    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
    while (iterator.hasNext()) {
      SelectionKey key = iterator.next();
      iterator.remove();
      if (!key.isReadable()) {
        continue;
      }
      byte[] payload = readChunk(state);
      if (payload != null) {
        return payload;
      }
    }
    return null;
  }

  private byte[] readChunk(ReadState state) throws IOException {
    if (state.payloadBuffer == null) {
      int read = channel.read(state.lengthBuffer);
      if (read < 0) {
        throw new IOException("Сервер закрыл соединение при чтении длины ответа");
      }
      if (state.lengthBuffer.hasRemaining()) {
        return null;
      }
      state.lengthBuffer.flip();
      int payloadLength = state.lengthBuffer.getInt();
      if (payloadLength < 0 || payloadLength > MAX_RESPONSE_SIZE) {
        throw new IOException("Некорректный размер ответа: " + payloadLength);
      }
      state.payloadBuffer = ByteBuffer.allocate(payloadLength);
    }
    int read = channel.read(state.payloadBuffer);
    if (read < 0) {
      throw new IOException("Сервер закрыл соединение при чтении payload ответа");
    }
    if (state.payloadBuffer.hasRemaining()) {
      return null;
    }
    return state.payloadBuffer.array();
  }

  private static HandshakeMessage deserializeHandshake(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, HandshakeMessage.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать handshake-фрейм", e);
    }
  }

  private static CommandResponse deserializeResponse(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, CommandResponse.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать ответ сервера", e);
    }
  }

  private void closeQuietly() {
    serverCertificate = null;
    sessionCipher = null;
    channelKey = null;
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException e) {
        LOGGER.debug("Не удалось закрыть канал клиента", e);
      }
      channel = null;
    }
    if (selector != null) {
      try {
        selector.close();
      } catch (IOException e) {
        LOGGER.debug("Не удалось закрыть Selector клиента", e);
      }
      selector = null;
    }
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static final class ReadState {
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    private ByteBuffer payloadBuffer;
  }
}
