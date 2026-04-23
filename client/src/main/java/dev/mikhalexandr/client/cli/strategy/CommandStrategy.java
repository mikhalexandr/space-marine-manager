package dev.mikhalexandr.client.cli.strategy;

@FunctionalInterface
public interface CommandStrategy {
  boolean handle(StrategyActions actions);
}
