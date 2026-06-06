package dev.mikhalexandr.client.app;

import dev.mikhalexandr.client.gui.SpaceMarineApp;

public final class Launcher {
  private Launcher() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static void main(String[] args) {
    SpaceMarineApp.main(args);
  }
}
