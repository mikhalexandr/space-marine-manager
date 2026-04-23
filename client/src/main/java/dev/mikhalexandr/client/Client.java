package dev.mikhalexandr.client;

import dev.mikhalexandr.client.app.ClientApplication;

/** Точка входа в клиентское приложение. */
public final class Client {
  private Client() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static void main(String[] args) {
    new ClientApplication().start(args);
  }
}
