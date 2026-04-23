package dev.mikhalexandr.client.cli.strategy;

public final class UnsupportedClientCommandStrategy implements CommandStrategy {
  private static final String CLIENT_UNAVAILABLE_MESSAGE = "Эта команда недоступна в клиенте";

  @Override
  public boolean handle(StrategyActions actions) {
    actions.printResponseLine(CLIENT_UNAVAILABLE_MESSAGE);
    return true;
  }
}
