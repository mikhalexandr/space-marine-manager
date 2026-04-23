package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.dto.request.CommandRequest;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.managers.CommandExecutor;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Однопоточный TCP-сервер, принимающий сериализованные командные запросы. */
public class TcpCommandServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TcpCommandServer.class);

  private final int port;
  private final CommandExecutor commandExecutor;

  public TcpCommandServer(int port, CommandExecutor commandExecutor) {
    this.port = port;
    this.commandExecutor = commandExecutor;
  }

  /** Запускает серверный цикл обработки подключений. */
  public void run() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      LOGGER.info("Сервер запущен и слушает TCP-порт {}", port);
      while (!Thread.currentThread().isInterrupted()) {
        try (Socket socket = serverSocket.accept()) {
          LOGGER.debug("Новое подключение: {}", socket.getRemoteSocketAddress());
          handleClient(socket);
        } catch (IOException e) {
          LOGGER.warn("Ошибка обработки клиентского подключения", e);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Не удалось запустить TCP-сервер", e);
    }
  }

  private void handleClient(Socket socket) throws IOException {
    InputStream inputStream = socket.getInputStream();
    OutputStream outputStream = socket.getOutputStream();

    while (true) {
      CommandRequest request;
      try {
        request = readRequest(inputStream);
      } catch (EOFException e) {
        LOGGER.debug("Клиент отключился: {}", socket.getRemoteSocketAddress());
        return;
      }

      String command = request == null ? "<null>" : request.getCommandType().getWireName();
      String requestId = request == null ? "<null>" : request.getRequestId();

      LOGGER.info(
          "Получен новый запрос от {}: command={}, requestId={}",
          socket.getRemoteSocketAddress(),
          command,
          requestId);

      CommandResponse response = executeRequest(request);
      writeResponse(outputStream, response);

      LOGGER.info(
          "Ответ отправлен {}: command={}, requestId={}, success={}, dataSize={}, messageLength={}",
          socket.getRemoteSocketAddress(),
          command,
          requestId,
          response.isSuccess(),
          response.getData() == null ? 0 : response.getData().size(),
          response.getMessage() == null ? 0 : response.getMessage().length());
    }
  }

  private static CommandRequest readRequest(InputStream inputStream) throws IOException {
    byte[] requestFrame = FrameCodec.readFrame(inputStream);
    try {
      return Serializer.deserialize(requestFrame, CommandRequest.class);
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать командный запрос", e);
    }
  }

  private CommandResponse executeRequest(CommandRequest request) {
    return commandExecutor.execute(request);
  }

  private static void writeResponse(OutputStream outputStream, CommandResponse response)
      throws IOException {
    byte[] responsePayload = Serializer.serialize(response);
    FrameCodec.writeFrame(outputStream, responsePayload);
  }
}
