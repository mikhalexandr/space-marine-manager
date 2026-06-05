package dev.mikhalexandr.client.gui;

import dev.mikhalexandr.client.network.TcpClient;
import dev.mikhalexandr.client.security.TrustAnchor;
import dev.mikhalexandr.common.dto.request.CommandType;
import dev.mikhalexandr.common.dto.response.CommandResponse;
import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.common.util.Env;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import programming.lab8.gui.Lab8Gui;
import programming.lab8.gui.api.CommandAction;
import programming.lab8.gui.api.CommandContext;
import programming.lab8.gui.api.GatewayResult;
import programming.lab8.gui.api.Lab8GuiConfig;
import programming.lab8.gui.ui.DialogStyler;

/** Точка входа гуи-клиента для коллекции SpaceMarine */
public final class SpaceMarineApp extends Application {
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 5050;
  private static final int MAX_RETRY_ATTEMPTS = 3;
  private static final long CONNECT_TIMEOUT_MILLIS = 2000L;
  private static final long REQUEST_TIMEOUT_MILLIS = 10000L;
  private static final String ENV_CA_CERT_PATH = "CA_CERT_PATH";
  private static final String DEFAULT_CA_CERT_PATH = "client/certs/ca.crt";
  private static final String BUNDLE = "dev.mikhalexandr.client.gui.i18n.marine";
  private static final int RESPONSE_DIALOG_COLUMNS = 72;
  private static final int RESPONSE_DIALOG_ROWS = 16;

  private TcpClient tcpClient;

  @Override
  public void start(Stage stage) {
    TcpClient client = buildClient(stage);
    if (client == null) {
      return;
    }
    this.tcpClient = client;
    SpaceMarineGateway gateway = new SpaceMarineGateway(client);
    SpaceMarineEditor editor = new SpaceMarineEditor();
    Lab8GuiConfig<SpaceMarine> config =
        Lab8GuiConfig.builder(gateway, new SpaceMarineDescriptor(), editor)
            .applicationTitle("app.title", "Space Marine Manager")
            .collectionItemName("collection.item.spaceMarine", "Space Marine")
            .resourceBundleBaseName(BUNDLE)
            .commands(commands(gateway, editor))
            .build();
    new Lab8Gui().start(stage, config);
  }

  @Override
  public void stop() {
    if (tcpClient != null) {
      tcpClient.close();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }

  private TcpClient buildClient(Stage stage) {
    List<String> raw = getParameters().getRaw();
    String host = raw.isEmpty() ? Env.orDefault("SERVER_HOST", DEFAULT_HOST) : raw.get(0);
    int port = raw.size() > 1 ? Integer.parseInt(raw.get(1)) : DEFAULT_PORT;
    String caCertPath = Env.orDefault(ENV_CA_CERT_PATH, DEFAULT_CA_CERT_PATH);
    try {
      TrustAnchor trustAnchor = TrustAnchor.loadFromFile(Path.of(caCertPath));
      return new TcpClient(
          host,
          port,
          MAX_RETRY_ATTEMPTS,
          CONNECT_TIMEOUT_MILLIS,
          REQUEST_TIMEOUT_MILLIS,
          trustAnchor);
    } catch (IOException | IllegalArgumentException e) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.initOwner(stage);
      alert.setHeaderText(null);
      alert.setGraphic(null);
      alert.setContentText("Не удалось запустить клиент: " + e.getMessage());
      DialogStyler.apply(alert, "lab8-alert-dialog");
      alert.showAndWait();
      return null;
    }
  }

  private static List<CommandAction<SpaceMarine>> commands(
      SpaceMarineGateway gateway, SpaceMarineEditor editor) {
    return List.of(
        serverOutput("help", "command.help", CommandType.HELP, gateway),
        serverOutput("info", "command.info", CommandType.INFO, gateway),
        serverOutput("history", "command.history", CommandType.HISTORY, gateway),
        serverOutput("head", "command.head", CommandType.HEAD, gateway),
        serverOutput("sum_of_health", "command.sumOfHealth", CommandType.SUM_OF_HEALTH, gateway),
        serverOutput("max_by_chapter", "command.maxByChapter", CommandType.MAX_BY_CHAPTER, gateway),
        clearCommand(gateway),
        addIfMinCommand(gateway, editor),
        countByCategoryCommand(gateway),
        executeScriptCommand(gateway));
  }

  private static CommandAction<SpaceMarine> serverOutput(
      String key, String labelKey, CommandType type, SpaceMarineGateway gateway) {
    return new CommandAction<>(
        key,
        labelKey,
        false,
        false,
        context -> showResponse(context, labelKey, gateway.execute(type)));
  }

  private static CommandAction<SpaceMarine> clearCommand(SpaceMarineGateway gateway) {
    return new CommandAction<>(
        "clear",
        "command.clear",
        false,
        true,
        context -> {
          if (!confirm(context)) {
            return GatewayResult.success(context.localization().message("status.cancelled"));
          }
          return showResponse(context, "command.clear", gateway.execute(CommandType.CLEAR));
        });
  }

  private static CommandAction<SpaceMarine> addIfMinCommand(
      SpaceMarineGateway gateway, SpaceMarineEditor editor) {
    return new CommandAction<>(
        "add_if_min",
        "command.addIfMin",
        false,
        true,
        context -> runAddIfMin(gateway, editor, context));
  }

  private static CommandAction<SpaceMarine> countByCategoryCommand(SpaceMarineGateway gateway) {
    return new CommandAction<>(
        "count_by_category",
        "command.countByCategory",
        false,
        false,
        context -> runCountByCategory(gateway, context));
  }

  private static CommandAction<SpaceMarine> executeScriptCommand(SpaceMarineGateway gateway) {
    return new CommandAction<>(
        "execute_script",
        "command.executeScript",
        false,
        true,
        context -> {
          File file =
              callOnFxThread(
                  () -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle(context.localization().message("dialog.chooseScript"));
                    return chooser.showOpenDialog(context.owner());
                  });
          if (file == null) {
            return GatewayResult.success(context.localization().message("status.cancelled"));
          }
          GatewayResult result = gateway.executeScript(file.toPath());
          return showGatewayResult(context, "command.executeScript", result);
        });
  }

  private static GatewayResult showResponse(
      CommandContext<SpaceMarine> context, String titleKey, CommandResponse response)
      throws Exception {
    GatewayResult result = SpaceMarineGateway.toResult(response);
    if (!result.success()) {
      return result;
    }
    StringBuilder text = new StringBuilder();
    if (response.getMessage() != null && !response.getMessage().isBlank()) {
      text.append(response.getMessage());
    }
    if (response.getData() != null && !response.getData().isEmpty()) {
      response
          .getData()
          .forEach(
              marine -> {
                if (!text.isEmpty()) {
                  text.append(System.lineSeparator()).append(System.lineSeparator());
                }
                text.append(marine);
              });
    }
    showText(context, titleKey, text.toString());
    return GatewayResult.success("");
  }

  private static void showText(CommandContext<SpaceMarine> context, String titleKey, String message)
      throws Exception {
    callOnFxThread(
        () -> {
          showResultDialog(context, titleKey, message);
          return null;
        });
  }

  private static void showResultDialog(
      CommandContext<SpaceMarine> context, String titleKey, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.initOwner(context.owner());
    alert.setTitle(context.localization().message(titleKey, titleKey));
    alert.setHeaderText(null);
    alert.setGraphic(null);
    TextArea text = new TextArea(message == null || message.isBlank() ? "-" : message);
    text.getStyleClass().add("lab8-result-output");
    text.setEditable(false);
    text.setWrapText(true);
    text.setPrefColumnCount(RESPONSE_DIALOG_COLUMNS);
    text.setPrefRowCount(RESPONSE_DIALOG_ROWS);
    alert.getDialogPane().setContent(text);
    DialogStyler.apply(alert, context.localization(), "lab8-result-dialog");
    alert.showAndWait();
  }

  private static GatewayResult runAddIfMin(
      SpaceMarineGateway gateway, SpaceMarineEditor editor, CommandContext<SpaceMarine> context)
      throws Exception {
    var marine = callOnFxThread(() -> editor.create(context.owner(), context.localization()));
    if (marine.isEmpty()) {
      return GatewayResult.success(context.localization().message("status.cancelled"));
    }
    GatewayResult result = gateway.addIfMin(marine.get());
    return showGatewayResult(context, "command.addIfMin", result);
  }

  private static GatewayResult runCountByCategory(
      SpaceMarineGateway gateway, CommandContext<SpaceMarine> context) throws Exception {
    var category = chooseCategory(context);
    if (category.isEmpty()) {
      return GatewayResult.success(context.localization().message("status.cancelled"));
    }
    GatewayResult result = gateway.countByCategory(category.get());
    return showGatewayResult(context, "command.countByCategory", result);
  }

  private static GatewayResult showGatewayResult(
      CommandContext<SpaceMarine> context, String titleKey, GatewayResult result) throws Exception {
    if (!result.success()) {
      return result;
    }
    showText(context, titleKey, result.message());
    return GatewayResult.success("");
  }

  private static Optional<AstartesCategory> chooseCategory(CommandContext<SpaceMarine> context)
      throws Exception {
    return callOnFxThread(
        () -> {
          ChoiceDialog<AstartesCategory> dialog =
              new ChoiceDialog<>(AstartesCategory.SCOUT, AstartesCategory.values());
          dialog.initOwner(context.owner());
          dialog.setTitle(context.localization().message("command.countByCategory"));
          dialog.setHeaderText(null);
          dialog.setGraphic(null);
          dialog.setContentText(context.localization().message("dialog.chooseCategory"));
          DialogStyler.apply(dialog, context.localization(), "lab8-dialog");
          return dialog.showAndWait();
        });
  }

  private static boolean confirm(CommandContext<SpaceMarine> context) throws Exception {
    return callOnFxThread(
        () -> {
          Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
          alert.initOwner(context.owner());
          alert.setTitle(context.localization().message("command.clear", "command.clear"));
          alert.setHeaderText(null);
          alert.setGraphic(null);
          alert.setContentText(
              context.localization().message("dialog.confirmClear", "dialog.confirmClear"));
          DialogStyler.apply(alert, context.localization(), "lab8-alert-dialog");
          return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
        });
  }

  private static <T> T callOnFxThread(Callable<T> action) throws Exception {
    if (Platform.isFxApplicationThread()) {
      return action.call();
    }
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    try {
      return task.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(cause);
    }
  }
}
