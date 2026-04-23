package dev.mikhalexandr.client.cli.strategy;

public final class ExecuteScriptCommandStrategy implements CommandStrategy {
  @Override
  public boolean handle(StrategyActions actions) {
    actions.pushScriptInput();
    return true;
  }
}
