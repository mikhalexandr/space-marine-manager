package dev.mikhalexandr.server.network;

import dev.mikhalexandr.server.managers.CommandExecutor;
import dev.mikhalexandr.server.security.ServerIdentity;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Многопоточный блокирующий TCP-сервер */
public final class TcpServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TcpServer.class);

  private final int port;
  private final CommandExecutor commandExecutor;
  private final ServerIdentity serverIdentity;
  private final ForkJoinPool processingPool;
  private final ForkJoinPool sendingPool;
  private final AtomicLong connectionCounter = new AtomicLong();
  private volatile boolean running;
  private volatile ServerSocket serverSocket;

  /**
   * @param port порт
   * @param commandExecutor исполнитель команд
   * @param serverIdentity серверная идентичность для рукопожатия
   */
  public TcpServer(int port, CommandExecutor commandExecutor, ServerIdentity serverIdentity) {
    this.port = port;
    this.commandExecutor = commandExecutor;
    this.serverIdentity = serverIdentity;
    this.processingPool = WorkerPools.newProcessingPool();
    this.sendingPool = WorkerPools.newSendingPool();
  }

  /** Запускает цикл приёма подключений */
  public void run() {
    running = true;
    try (ServerSocket socket = new ServerSocket(port)) {
      this.serverSocket = socket;
      LOGGER.info("Сервер запущен и слушает TCP-порт {}", port);
      acceptLoop(socket);
    } catch (IOException e) {
      throw new IllegalStateException("Не удалось запустить TCP-сервер на порту " + port, e);
    } finally {
      running = false;
      this.serverSocket = null;
      shutdownPools();
    }
  }

  /** Останавливает сервер: закрывает серверный сокет, чем прерывает цикл приёма подключений */
  public void stop() {
    running = false;
    ServerSocket socket = serverSocket;
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
        LOGGER.debug("Ошибка при закрытии серверного сокета", e);
      }
    }
  }

  private void acceptLoop(ServerSocket socket) {
    while (running && !Thread.currentThread().isInterrupted()) {
      try {
        startReaderThread(socket.accept());
      } catch (IOException e) {
        if (running) {
          LOGGER.warn("Не удалось принять подключение: {}", e.getMessage());
        }
      }
    }
  }

  private void startReaderThread(Socket client) {
    LOGGER.debug("Новое подключение: {}", client.getRemoteSocketAddress());
    ClientReader reader =
        new ClientReader(client, serverIdentity, commandExecutor, processingPool, sendingPool);
    Thread thread = new Thread(reader, "client-reader-" + connectionCounter.incrementAndGet());
    thread.setDaemon(true);
    thread.start();
  }

  private void shutdownPools() {
    processingPool.shutdown();
    sendingPool.shutdown();
  }
}
