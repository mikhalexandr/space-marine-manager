package dev.mikhalexandr.server.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Фабрика пула потоков для выполнения сервачковых команд вне selector loopа. */
final class TcpRequestWorkerPool {
  private TcpRequestWorkerPool() {}

  static ExecutorService create() {
    return Executors.newFixedThreadPool(resolveWorkerCount(), new WorkerFactory());
  }

  private static int resolveWorkerCount() {
    return Math.max(2, Runtime.getRuntime().availableProcessors());
  }

  private static final class WorkerFactory implements ThreadFactory {
    private int threadIndex = 1;

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "tcp-command-worker-" + threadIndex++);
      thread.setDaemon(true);
      return thread;
    }
  }
}
