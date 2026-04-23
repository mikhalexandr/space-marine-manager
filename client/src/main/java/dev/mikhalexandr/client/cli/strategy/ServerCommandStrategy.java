package dev.mikhalexandr.client.cli.strategy;

public final class ServerCommandStrategy implements CommandStrategy {
  @Override
  public boolean handle(StrategyActions actions) {
    actions.processRequest();
    return true;
  }
}
