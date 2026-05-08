package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.security.cert.CertificateUtils;
import dev.mikhalexandr.common.security.crypto.KeyAgreementService;
import dev.mikhalexandr.common.security.crypto.MessageSigner;
import dev.mikhalexandr.common.security.crypto.SessionCipher;
import dev.mikhalexandr.common.security.crypto.SessionKeys;
import dev.mikhalexandr.common.security.handshake.ClientHello;
import dev.mikhalexandr.common.security.handshake.HandshakeMessage;
import dev.mikhalexandr.common.security.handshake.ServerHello;
import dev.mikhalexandr.common.util.Bytes;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.network.ClientConnection.HandshakeStage;
import dev.mikhalexandr.server.security.ServerIdentity;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Неблокирующий TCP-сервачок на Selector с обработкой команд в пуле потоков. */
public class TcpServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TcpServer.class);
  private static final int MAX_FRAME_SIZE = 8 * 1024 * 1024;

  private final int port;
  private final CommandExecutor commandExecutor;
  private final ServerIdentity serverIdentity;
  private final ExecutorService requestExecutor;
  private volatile boolean running;
  private volatile Selector selector;

  public TcpServer(int port, CommandExecutor commandExecutor, ServerIdentity serverIdentity) {
    this.port = port;
    this.commandExecutor = commandExecutor;
    this.serverIdentity = serverIdentity;
    this.requestExecutor = TcpRequestWorkerPool.create();
  }

  /** Запускает серверный цикл обработки подключений. */
  public void run() {
    running = true;
    try (Selector serverSelector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
      this.selector = serverSelector;
      initServerChannel(serverChannel, serverSelector);
      LOGGER.info("Сервер запущен и слушает TCP-порт {}", port);
      eventLoop(serverSelector);
    } catch (IOException e) {
      shutdownExecutor();
      throw new IllegalStateException("Не удалось запустить TCP-сервер", e);
    } finally {
      running = false;
      this.selector = null;
    }
  }

  /** Останавливает серверный цикл и будит Selector, если он ждет новых событий. */
  public void stop() {
    running = false;
    Selector currentSelector = selector;
    if (currentSelector != null) {
      currentSelector.wakeup();
    }
  }

  private void initServerChannel(ServerSocketChannel serverChannel, Selector serverSelector)
      throws IOException {
    serverChannel.configureBlocking(false);
    serverChannel.bind(new InetSocketAddress(port));
    serverChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
  }

  private void eventLoop(Selector serverSelector) throws IOException {
    try {
      while (running && !Thread.currentThread().isInterrupted()) {
        serverSelector.select();
        processSelectedKeys(serverSelector);
      }
    } finally {
      shutdownExecutor();
      closeRegisteredChannels(serverSelector);
    }
  }

  private void processSelectedKeys(Selector serverSelector) throws IOException {
    Iterator<SelectionKey> iterator = serverSelector.selectedKeys().iterator();
    while (iterator.hasNext()) {
      SelectionKey key = iterator.next();
      iterator.remove();
      if (!key.isValid()) {
        continue;
      }
      if (key.isAcceptable()) {
        acceptClient(serverSelector, key);
        continue;
      }
      if (key.isReadable()) {
        readFromClient(key);
      }
      if (key.isValid() && key.isWritable()) {
        writeToClient(key);
      }
    }
  }

  private void acceptClient(Selector serverSelector, SelectionKey key) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel clientChannel = serverChannel.accept();
    if (clientChannel == null) {
      return;
    }

    clientChannel.configureBlocking(false);
    ClientConnection connection = new ClientConnection(clientChannel.getRemoteAddress());
    clientChannel.register(serverSelector, SelectionKey.OP_READ, connection);
    LOGGER.debug("Новое подключение: {}", connection.remoteAddress());
  }

  private void readFromClient(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ClientConnection connection = connectionOf(key);

    if (!readFrame(channel, connection)) {
      return;
    }

    byte[] payload = connection.takePayload();
    connection.markRequestInFlight();
    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);

    if (connection.handshakeStage() == HandshakeStage.ESTABLISHED) {
      dispatchRequest(key, payload, connection);
      return;
    }
    dispatchHandshake(key, payload, connection);
  }

  private boolean readFrame(SocketChannel channel, ClientConnection connection) throws IOException {
    if (connection.payloadBuffer() == null) {
      return readFrameLength(channel, connection);
    }
    return readFramePayload(channel, connection);
  }

  private boolean readFrameLength(SocketChannel channel, ClientConnection connection)
      throws IOException {
    int readBytes = channel.read(connection.lengthBuffer());
    if (readBytes < 0) {
      closeClient(channel, connection);
      return false;
    }
    if (connection.lengthBuffer().hasRemaining()) {
      return false;
    }

    connection.lengthBuffer().flip();
    int payloadLength = connection.lengthBuffer().getInt();
    if (payloadLength < 0 || payloadLength > MAX_FRAME_SIZE) {
      throw new IOException("Некорректный размер фрейма: " + payloadLength);
    }
    connection.preparePayloadBuffer(payloadLength);
    return readFramePayload(channel, connection);
  }

  private boolean readFramePayload(SocketChannel channel, ClientConnection connection)
      throws IOException {
    int readBytes = channel.read(connection.payloadBuffer());
    if (readBytes < 0) {
      closeClient(channel, connection);
      return false;
    }
    return !connection.payloadBuffer().hasRemaining();
  }

  private CommandRequest deserializeRequest(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, CommandRequest.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать командный запрос", e);
    }
  }

  private void dispatchHandshake(SelectionKey key, byte[] payload, ClientConnection connection) {
    String remoteAddress = connection.remoteAddress();
    LOGGER.info("Получен handshake-фрейм от {}, payload={} байт", remoteAddress, payload.length);
    requestExecutor.submit(() -> processHandshakeAsync(key, payload, connection, remoteAddress));
  }

  private void processHandshakeAsync(
      SelectionKey key, byte[] payload, ClientConnection connection, String remoteAddress) {
    try {
      HandshakeMessage message = deserializeHandshake(payload);
      handleClientHello(key, connection, message.asClientHello(), remoteAddress);
    } catch (IOException e) {
      LOGGER.warn("Ошибка handshake с {}: {}", remoteAddress, e.getMessage());
      closeKey(key);
    }
  }

  private void handleClientHello(
      SelectionKey key, ClientConnection connection, ClientHello hello, String remoteAddress)
      throws IOException {
    LOGGER.info("ClientHello от {}: clientId={}", remoteAddress, hello.clientId());
    PublicKey clientEphemeral = KeyAgreementService.decodePublicKey(hello.ephemeralPublicKey());
    KeyPair serverEphemeral = KeyAgreementService.generateEphemeralKeyPair();
    byte[] serverEphemeralEncoded =
        KeyAgreementService.encodePublicKey(serverEphemeral.getPublic());

    byte[] transcript = Bytes.concat(hello.ephemeralPublicKey(), serverEphemeralEncoded);
    byte[] signature = MessageSigner.sign(transcript, serverIdentity.privateKey());

    byte[] sharedSecret =
        KeyAgreementService.computeSharedSecret(serverEphemeral.getPrivate(), clientEphemeral);
    byte[] transcriptHash =
        KeyAgreementService.transcriptHash(hello.ephemeralPublicKey(), serverEphemeralEncoded);
    SessionKeys keys = KeyAgreementService.deriveSessionKeys(sharedSecret, transcriptHash);
    SessionCipher cipher = new SessionCipher(keys.serverToClient(), keys.clientToServer());

    ServerHello reply =
        new ServerHello(
            CertificateUtils.encodeCertificate(serverIdentity.certificate()),
            serverEphemeralEncoded,
            signature);
    byte[] replyBytes = Serializer.serialize(reply);
    connection.enqueueResponse(ByteBuffer.wrap(FrameCodec.toFrame(replyBytes)));
    connection.markEstablished(cipher);
    key.interestOps(SelectionKey.OP_WRITE);
    key.selector().wakeup();
    LOGGER.info(
        "ServerHello отправлен {}, ECDH согласован, сессия зашифрована (frame={} байт)",
        remoteAddress,
        replyBytes.length);
  }

  private static HandshakeMessage deserializeHandshake(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, HandshakeMessage.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать handshake-фрейм", e);
    }
  }

  private void dispatchRequest(
      SelectionKey key, byte[] encryptedPayload, ClientConnection connection) {
    String remoteAddress = connection.remoteAddress();
    requestExecutor.submit(
        () -> processRequestAsync(key, encryptedPayload, connection, remoteAddress));
  }

  private void processRequestAsync(
      SelectionKey key,
      byte[] encryptedPayload,
      ClientConnection connection,
      String remoteAddress) {
    try {
      byte[] plaintext = connection.sessionCipher().decrypt(encryptedPayload);
      CommandRequest request = deserializeRequest(plaintext);
      String command = request == null ? "<null>" : request.getCommandType().getWireName();
      String requestId = request == null ? "<null>" : request.getRequestId();
      LOGGER.info(
          "Получен новый запрос от {}: command={}, requestId={}",
          remoteAddress,
          command,
          requestId);
      CommandResponse response = commandExecutor.execute(request);
      enqueueEncryptedResponse(key, connection, response, remoteAddress, command, requestId);
    } catch (IOException e) {
      LOGGER.warn("Не удалось обработать запрос для {}", remoteAddress, e);
      closeKey(key);
    } catch (RuntimeException e) {
      LOGGER.warn("Ошибка выполнения команды для {}", remoteAddress, e);
      closeKey(key);
    }
  }

  private void enqueueEncryptedResponse(
      SelectionKey key,
      ClientConnection connection,
      CommandResponse response,
      String remoteAddress,
      String command,
      String requestId)
      throws IOException {
    byte[] responseBytes = Serializer.serialize(response);
    byte[] encrypted = connection.sessionCipher().encrypt(responseBytes);
    connection.enqueueResponse(ByteBuffer.wrap(FrameCodec.toFrame(encrypted)));
    key.interestOps(SelectionKey.OP_WRITE);
    key.selector().wakeup();

    LOGGER.info(
        "Ответ подготовлен {}: command={}, requestId={}, success={}, dataSize={}, messageLength={}",
        remoteAddress,
        command,
        requestId,
        response.isSuccess(),
        response.getData() == null ? 0 : response.getData().size(),
        response.getMessage() == null ? 0 : response.getMessage().length());
  }

  private void writeToClient(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ClientConnection connection = connectionOf(key);
    ByteBuffer responseBuffer = connection.responseBuffer();
    if (responseBuffer == null) {
      key.interestOps(connection.requestInFlight() ? 0 : SelectionKey.OP_READ);
      return;
    }

    channel.write(responseBuffer);
    if (responseBuffer.hasRemaining()) {
      return;
    }

    connection.clearResponseBuffer();
    connection.completeRequest();
    key.interestOps(SelectionKey.OP_READ);
  }

  private ClientConnection connectionOf(SelectionKey key) {
    return (ClientConnection) key.attachment();
  }

  private void closeClient(SocketChannel channel, ClientConnection connection) throws IOException {
    LOGGER.debug("Клиент отключился: {}", connection.remoteAddress());
    channel.close();
  }

  private void closeKey(SelectionKey key) {
    try {
      key.channel().close();
    } catch (IOException e) {
      LOGGER.debug("Не удалось закрыть канал", e);
    }
    key.cancel();
  }

  private void closeRegisteredChannels(Selector serverSelector) {
    for (SelectionKey key : serverSelector.keys()) {
      closeKey(key);
    }
  }

  private void shutdownExecutor() {
    requestExecutor.shutdownNow();
  }
}
