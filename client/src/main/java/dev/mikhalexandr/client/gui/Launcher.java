package dev.mikhalexandr.client.gui;

/** Обёртка для запуска из фэт джар */
public final class Launcher {
  private Launcher() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static void main(String[] args) {
    SpaceMarineApp.main(args);
  }
}
