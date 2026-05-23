package dev.mikhalexandr.server.network;

import java.util.concurrent.ForkJoinPool;

/** Фабрика пулов {@link ForkJoinPool} для многопоточной обработки и отправки ответов */
final class WorkerPools {
  private static final int MIN_PARALLELISM = 2;

  private WorkerPools() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * @return пул для обработки полученных запросов
   */
  static ForkJoinPool newProcessingPool() {
    return new ForkJoinPool(parallelism());
  }

  /**
   * @return пул для отправки ответов клиентам
   */
  static ForkJoinPool newSendingPool() {
    return new ForkJoinPool(parallelism());
  }

  private static int parallelism() {
    return Math.max(MIN_PARALLELISM, Runtime.getRuntime().availableProcessors());
  }
}
