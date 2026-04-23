package dev.mikhalexandr.client.network;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.util.Serializer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/** Неблокирующий TCP-клиент для обмена командами с сервером. */
public class TcpClient {
  private static final int MAX_RESPONSE_SIZE = 8 * 1024 * 1024;
  private static final long RETRY_BACKOFF_STEP_MILLIS = 300L;
  private static final int SELECT_TIMEOUT_MILLIS = 200;

  private final String host;
  private final int port;
  private final int maxAttempts;
  private final long connectTimeoutMillis;
  private final long requestTimeoutMillis;

  public TcpClient(
      String host,
      int port,
      int maxAttempts,
      long connectTimeoutMillis,
      long requestTimeoutMillis) {
    this.host = host;
    this.port = port;
    this.maxAttempts = maxAttempts;
    this.connectTimeoutMillis = connectTimeoutMillis;
    this.requestTimeoutMillis = requestTimeoutMillis;
  }

  /**
   * Отправляет запрос на сервер с retry при временной недоступности.
   *
   * @param request запрос команды
   * @return ответ сервера
   */
  public CommandResponse send(CommandRequest request) throws IOException {
    IOException lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return sendOnce(request);
      } catch (IOException e) {
        lastException = e;
        if (attempt < maxAttempts) {
          sleepQuietly(attempt * RETRY_BACKOFF_STEP_MILLIS);
        }
      }
    }

    throw new IOException(
        String.format("Сервер временно недоступен (%s:%d), попыток: %d", host, port, maxAttempts),
        lastException);
  }

  private CommandResponse sendOnce(CommandRequest request) throws IOException {
    ByteBuffer writeBuffer = ByteBuffer.wrap(FrameCodec.toFrame(serializeRequest(request)));
    ReadState readState = new ReadState();
    try (Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open()) {
      initChannel(channel, selector);
      long connectDeadline = System.currentTimeMillis() + connectTimeoutMillis;
      long requestDeadline = System.currentTimeMillis() + requestTimeoutMillis;
      return waitForResponse(
          selector, channel, writeBuffer, readState, connectDeadline, requestDeadline);
    }
  }

  private static byte[] serializeRequest(CommandRequest request) throws IOException {
    try {
      return Serializer.serialize(request);
    } catch (IOException e) {
      throw new IOException("Не удалось сериализовать запрос", e);
    }
  }

  private void initChannel(SocketChannel channel, Selector selector) throws IOException {
    channel.configureBlocking(false);
    channel.connect(new InetSocketAddress(host, port));
    channel.register(selector, SelectionKey.OP_CONNECT);
  }

  private CommandResponse waitForResponse(
      Selector selector,
      SocketChannel channel,
      ByteBuffer writeBuffer,
      ReadState readState,
      long connectDeadline,
      long requestDeadline)
      throws IOException {
    while (System.currentTimeMillis() < requestDeadline) {
      ensureConnectTimeout(channel, connectDeadline);
      selector.select(SELECT_TIMEOUT_MILLIS);
      CommandResponse response = processSelectedKeys(selector, channel, writeBuffer, readState);
      if (response != null) {
        return response;
      }
    }

    throw new IOException("Таймаут ожидания ответа от сервера");
  }

  private static void ensureConnectTimeout(SocketChannel channel, long connectDeadline)
      throws IOException {
    if (System.currentTimeMillis() > connectDeadline && !channel.isConnected()) {
      throw new IOException("Таймаут подключения к серверу");
    }
  }

  private static CommandResponse processSelectedKeys(
      Selector selector, SocketChannel channel, ByteBuffer writeBuffer, ReadState readState)
      throws IOException {
    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
    while (iterator.hasNext()) {
      SelectionKey key = iterator.next();
      iterator.remove();

      if (!key.isValid()) {
        continue;
      }

      CommandResponse response = processSingleKey(key, channel, writeBuffer, readState);
      if (response != null) {
        return response;
      }
    }
    return null;
  }

  private static CommandResponse processSingleKey(
      SelectionKey key, SocketChannel channel, ByteBuffer writeBuffer, ReadState readState)
      throws IOException {
    if (key.isConnectable()) {
      finishConnect(channel, key);
    }
    if (key.isWritable()) {
      writeRequest(channel, key, writeBuffer);
    }
    if (!key.isReadable()) {
      return null;
    }
    return readResponse(channel, readState);
  }

  private static void writeRequest(SocketChannel channel, SelectionKey key, ByteBuffer writeBuffer)
      throws IOException {
    channel.write(writeBuffer);
    if (!writeBuffer.hasRemaining()) {
      key.interestOps(SelectionKey.OP_READ);
    }
  }

  private static CommandResponse readResponse(SocketChannel channel, ReadState readState)
      throws IOException {
    if (readState.payloadBuffer == null) {
      readResponseLength(channel, readState);
      return null;
    }
    return readResponsePayload(channel, readState);
  }

  private static void readResponseLength(SocketChannel channel, ReadState readState)
      throws IOException {
    int readBytes = channel.read(readState.lengthBuffer);
    if (readBytes < 0) {
      throw new IOException("Сервер закрыл соединение при чтении длины ответа");
    }
    if (readState.lengthBuffer.hasRemaining()) {
      return;
    }

    readState.lengthBuffer.flip();
    int payloadLength = readState.lengthBuffer.getInt();
    if (payloadLength < 0 || payloadLength > MAX_RESPONSE_SIZE) {
      throw new IOException("Некорректный размер ответа: " + payloadLength);
    }
    readState.payloadBuffer = ByteBuffer.allocate(payloadLength);
  }

  private static CommandResponse readResponsePayload(SocketChannel channel, ReadState readState)
      throws IOException {
    int readBytes = channel.read(readState.payloadBuffer);
    if (readBytes < 0) {
      throw new IOException("Сервер закрыл соединение при чтении payload ответа");
    }
    if (readState.payloadBuffer.hasRemaining()) {
      return null;
    }
    return deserializeResponse(readState.payloadBuffer.array());
  }

  private static void finishConnect(SocketChannel channel, SelectionKey key) throws IOException {
    if (channel.finishConnect()) {
      key.interestOps(SelectionKey.OP_WRITE);
      return;
    }
    throw new IOException("Не удалось завершить установку соединения");
  }

  private static CommandResponse deserializeResponse(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, CommandResponse.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать ответ сервера", e);
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
