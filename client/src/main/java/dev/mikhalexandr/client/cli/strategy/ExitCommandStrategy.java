package dev.mikhalexandr.client.cli.strategy;

public final class ExitCommandStrategy implements CommandStrategy {
  @Override
  public boolean handle(StrategyActions actions) {
    return false;
  }
}
