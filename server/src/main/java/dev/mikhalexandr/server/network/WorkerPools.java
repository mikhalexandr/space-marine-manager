package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.util.Env;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

final class WorkerPools {
  private static final String ENV_PROCESSING_THREADS = "SERVER_PROCESSING_THREADS";
  private static final String ENV_SENDING_THREADS = "SERVER_SENDING_THREADS";
  private static final int MIN_THREADS = 2;
  private static final int MIN_PROCESSING_FALLBACK = 8;

  private WorkerPools() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  static ExecutorService newProcessingPool() {
    int fallback = Math.max(MIN_PROCESSING_FALLBACK, Runtime.getRuntime().availableProcessors());
    return Executors.newFixedThreadPool(
        threads(ENV_PROCESSING_THREADS, fallback), namedDaemonFactory("server-processing"));
  }

  static ExecutorService newSendingPool() {
    int fallback = Math.max(MIN_THREADS, Runtime.getRuntime().availableProcessors());
    return Executors.newFixedThreadPool(
        threads(ENV_SENDING_THREADS, fallback), namedDaemonFactory("server-sending"));
  }

  private static int threads(String envName, int fallback) {
    return Math.max(MIN_THREADS, (int) Env.longOrDefault(envName, fallback));
  }

  private static ThreadFactory namedDaemonFactory(String prefix) {
    AtomicLong counter = new AtomicLong();
    return runnable -> {
      Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    };
  }
}
