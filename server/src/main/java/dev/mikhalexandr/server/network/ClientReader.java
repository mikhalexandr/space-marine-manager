package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.security.crypto.SessionCipher;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.security.ServerIdentity;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Поток чтения запросов одного клиента */
final class ClientReader implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientReader.class);

  private final Socket socket;
  private final ServerIdentity serverIdentity;
  private final CommandExecutor commandExecutor;
  private final ForkJoinPool processingPool;
  private final ForkJoinPool sendingPool;

  ClientReader(
      Socket socket,
      ServerIdentity serverIdentity,
      CommandExecutor commandExecutor,
      ForkJoinPool processingPool,
      ForkJoinPool sendingPool) {
    this.socket = socket;
    this.serverIdentity = serverIdentity;
    this.commandExecutor = commandExecutor;
    this.processingPool = processingPool;
    this.sendingPool = sendingPool;
  }

  @Override
  public void run() {
    String remote = String.valueOf(socket.getRemoteSocketAddress());
    try {
      InputStream input = new BufferedInputStream(socket.getInputStream());
      OutputStream output = new BufferedOutputStream(socket.getOutputStream());
      SessionCipher cipher = ServerHandshake.perform(input, output, serverIdentity);
      LOGGER.info("Защищённая сессия установлена с {}", remote);
      readLoop(new ClientConnection(socket, input, output, cipher, remote));
    } catch (IOException e) {
      LOGGER.info("Соединение с {} завершено: {}", remote, e.getMessage());
    } finally {
      closeQuietly(remote);
    }
  }

  private void readLoop(ClientConnection connection) throws IOException {
    while (connection.isOpen()) {
      byte[] frame = FrameCodec.readFrame(connection.input());
      processingPool.execute(() -> processRequest(connection, frame));
    }
  }

  private void processRequest(ClientConnection connection, byte[] encryptedFrame) {
    try {
      byte[] plaintext = connection.cipher().decrypt(encryptedFrame);
      CommandRequest request = deserializeRequest(plaintext);
      CommandResponse response = executeSafely(request);
      byte[] encrypted = connection.cipher().encrypt(Serializer.serialize(response));
      sendingPool.execute(() -> sendResponse(connection, encrypted));
    } catch (IOException e) {
      LOGGER.warn(
          "Не удалось обработать запрос {}: {}", connection.remoteAddress(), e.getMessage());
      connection.close();
    }
  }

  private CommandResponse executeSafely(CommandRequest request) {
    try {
      return commandExecutor.execute(request);
    } catch (RuntimeException e) {
      LOGGER.error("Непредвиденная ошибка при обработке команды", e);
      return CommandResponse.error("Внутренняя ошибка сервера: " + e.getMessage());
    }
  }

  private static void sendResponse(ClientConnection connection, byte[] responsePayload) {
    try {
      connection.writeFrame(responsePayload);
    } catch (IOException e) {
      LOGGER.warn("Не удалось отправить ответ {}: {}", connection.remoteAddress(), e.getMessage());
      connection.close();
    }
  }

  private static CommandRequest deserializeRequest(byte[] payload) throws IOException {
    try {
      return Serializer.deserialize(payload, CommandRequest.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать запрос команды", e);
    }
  }

  private void closeQuietly(String remote) {
    try {
      socket.close();
    } catch (IOException e) {
      LOGGER.debug("Не удалось закрыть сокет {}", remote, e);
    }
  }
}
