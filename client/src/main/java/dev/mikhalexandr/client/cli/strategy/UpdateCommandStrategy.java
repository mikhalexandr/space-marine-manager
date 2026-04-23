package dev.mikhalexandr.client.cli.strategy;

public final class UpdateCommandStrategy implements CommandStrategy {
  @Override
  public boolean handle(StrategyActions actions) {
    if (actions.ensureUpdateTargetExists()) {
      actions.processRequest();
    }
    return true;
  }
}
