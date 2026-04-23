package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.managers.CommandExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Неблокирующий TCP-сервачок на Selector с обработкой команд в пуле потоков. */
public class TcpServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TcpServer.class);
  private static final int MAX_FRAME_SIZE = 8 * 1024 * 1024;
  private static final int SELECT_TIMEOUT_MILLIS = 200;

  private final int port;
  private final CommandExecutor commandExecutor;
  private final ExecutorService requestExecutor;

  public TcpServer(int port, CommandExecutor commandExecutor) {
    this.port = port;
    this.commandExecutor = commandExecutor;
    this.requestExecutor = TcpRequestWorkerPool.create();
  }

  /** Запускает серверный цикл обработки подключений. */
  public void run() {
    try (Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
      initServerChannel(serverChannel, selector);
      LOGGER.info("Сервер запущен и слушает TCP-порт {}", port);
      eventLoop(selector);
    } catch (IOException e) {
      shutdownExecutor();
      throw new IllegalStateException("Не удалось запустить TCP-сервер", e);
    }
  }

  private void initServerChannel(ServerSocketChannel serverChannel, Selector selector)
      throws IOException {
    serverChannel.configureBlocking(false);
    serverChannel.bind(new InetSocketAddress(port));
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
  }

  private void eventLoop(Selector selector) throws IOException {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        selector.select(SELECT_TIMEOUT_MILLIS);
        processSelectedKeys(selector);
      }
    } finally {
      shutdownExecutor();
      closeRegisteredChannels(selector);
    }
  }

  private void processSelectedKeys(Selector selector) throws IOException {
    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
    while (iterator.hasNext()) {
      SelectionKey key = iterator.next();
      iterator.remove();
      if (!key.isValid()) {
        continue;
      }
      if (key.isAcceptable()) {
        acceptClient(selector, key);
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

  private void acceptClient(Selector selector, SelectionKey key) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel clientChannel = serverChannel.accept();
    if (clientChannel == null) {
      return;
    }

    clientChannel.configureBlocking(false);
    ClientConnection connection = new ClientConnection(clientChannel.getRemoteAddress());
    clientChannel.register(selector, SelectionKey.OP_READ, connection);
    LOGGER.debug("Новое подключение: {}", connection.remoteAddress());
  }

  private void readFromClient(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ClientConnection connection = connectionOf(key);

    if (!readFrame(channel, connection)) {
      return;
    }

    CommandRequest request = deserializeRequest(connection.takePayload());
    connection.markRequestInFlight();
    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
    dispatchRequest(key, request, connection);
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

  private void dispatchRequest(
      SelectionKey key, CommandRequest request, ClientConnection connection) {
    String command = request == null ? "<null>" : request.getCommandType().getWireName();
    String requestId = request == null ? "<null>" : request.getRequestId();
    LOGGER.info(
        "Получен новый запрос от {}: command={}, requestId={}",
        connection.remoteAddress(),
        command,
        requestId);

    requestExecutor.submit(
        () -> processRequestAsync(key, request, connection.remoteAddress(), command, requestId));
  }

  private void processRequestAsync(
      SelectionKey key,
      CommandRequest request,
      String remoteAddress,
      String command,
      String requestId) {
    try {
      CommandResponse response = commandExecutor.execute(request);
      enqueueResponse(key, response, remoteAddress, command, requestId);
    } catch (IOException e) {
      LOGGER.warn("Не удалось сериализовать ответ для {}", remoteAddress, e);
      closeKey(key);
    } catch (RuntimeException e) {
      LOGGER.warn("Ошибка выполнения команды для {}", remoteAddress, e);
      closeKey(key);
    }
  }

  private void enqueueResponse(
      SelectionKey key,
      CommandResponse response,
      String remoteAddress,
      String command,
      String requestId)
      throws IOException {
    ClientConnection connection = connectionOf(key);
    connection.enqueueResponse(ByteBuffer.wrap(FrameCodec.toFrame(serializeResponse(response))));
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

  private static byte[] serializeResponse(CommandResponse response) throws IOException {
    return Serializer.serialize(response);
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

  private void closeRegisteredChannels(Selector selector) {
    for (SelectionKey key : selector.keys()) {
      closeKey(key);
    }
  }

  private void shutdownExecutor() {
    requestExecutor.shutdownNow();
  }
}
