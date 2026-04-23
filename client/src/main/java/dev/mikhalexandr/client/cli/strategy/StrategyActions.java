package dev.mikhalexandr.client.cli.strategy;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class StrategyActions {
  private final Runnable requestProcessor;
  private final Runnable scriptInputPusher;
  private final BooleanSupplier updateTargetExistsChecker;
  private final Consumer<String> responsePrinter;

  public StrategyActions(
      Runnable requestProcessor,
      Runnable scriptInputPusher,
      BooleanSupplier updateTargetExistsChecker,
      Consumer<String> responsePrinter) {
    this.requestProcessor = requestProcessor;
    this.scriptInputPusher = scriptInputPusher;
    this.updateTargetExistsChecker = updateTargetExistsChecker;
    this.responsePrinter = responsePrinter;
  }

  public void processRequest() {
    requestProcessor.run();
  }

  public void pushScriptInput() {
    scriptInputPusher.run();
  }

  public boolean ensureUpdateTargetExists() {
    return updateTargetExistsChecker.getAsBoolean();
  }

  public void printResponseLine(String message) {
    responsePrinter.accept(message);
  }
}
