package dev.mikhalexandr.client.gui;

import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.Chapter;
import dev.mikhalexandr.common.models.Coordinates;
import dev.mikhalexandr.common.models.MeleeWeapon;
import dev.mikhalexandr.common.models.SpaceMarine;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import programming.lab8.gui.api.ObjectEditor;
import programming.lab8.gui.i18n.Localization;
import programming.lab8.gui.ui.DialogStyler;

public final class SpaceMarineEditor implements ObjectEditor<SpaceMarine> {

  @Override
  public Optional<SpaceMarine> create(Window owner, Localization localization) {
    return show(owner, localization, "dialog.addTitle", null);
  }

  @Override
  public Optional<SpaceMarine> edit(Window owner, Localization localization, SpaceMarine object) {
    return show(owner, localization, "dialog.editTitle", object);
  }

  private Optional<SpaceMarine> show(
      Window owner, Localization loc, String titleKey, SpaceMarine existing) {
    Form form = new Form();
    if (existing != null) {
      form.fill(existing);
    }
    Dialog<SpaceMarine> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle(loc.message(titleKey, titleKey));
    dialog.setHeaderText(null);
    dialog.getDialogPane().setContent(form.grid(loc));
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    SpaceMarine[] result = new SpaceMarine[1];
    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    okButton.addEventFilter(
        ActionEvent.ACTION,
        event -> {
          try {
            result[0] = form.toMarine(loc);
          } catch (RuntimeException e) {
            event.consume();
            showValidationError(dialog, loc, e.getMessage());
          }
        });
    dialog.setResultConverter(button -> button == ButtonType.OK ? result[0] : null);
    DialogStyler.apply(dialog, loc, "lab8-dialog");
    return dialog.showAndWait();
  }

  private static void showValidationError(Dialog<?> parent, Localization loc, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.initOwner(parent.getDialogPane().getScene().getWindow());
    alert.setTitle(loc.message("dialog.warning", "Warning"));
    alert.setHeaderText(null);
    alert.setGraphic(null);
    alert.setContentText(
        message == null || message.isBlank() ? loc.message("dialog.warning", "Warning") : message);
    DialogStyler.apply(alert, loc, "lab8-alert-dialog");
    alert.showAndWait();
  }

  private static final class Form {
    private static final double HGAP = 10;
    private static final double VGAP = 8;

    private final TextField name = new TextField();
    private final TextField coordinateX = new TextField();
    private final TextField coordinateY = new TextField();
    private final TextField health = new TextField();
    private final TextField height = new TextField();
    private final ComboBox<AstartesCategory> category = new ComboBox<>();
    private final ComboBox<MeleeWeapon> meleeWeapon = new ComboBox<>();
    private final TextField chapterName = new TextField();
    private final TextField chapterLegion = new TextField();

    private Form() {
      category.getItems().addAll(AstartesCategory.values());
      meleeWeapon.getItems().add(null);
      meleeWeapon.getItems().addAll(MeleeWeapon.values());
      category.setValue(AstartesCategory.SCOUT);
    }

    private GridPane grid(Localization loc) {
      GridPane grid = new GridPane();
      grid.setHgap(HGAP);
      grid.setVgap(VGAP);
      int row = 0;
      row = addRow(grid, loc, "field.name", name, row);
      row = addRow(grid, loc, "field.x", coordinateX, row);
      row = addRow(grid, loc, "field.y", coordinateY, row);
      row = addRow(grid, loc, "field.health", health, row);
      row = addRow(grid, loc, "field.height", height, row);
      row = addRow(grid, loc, "field.category", category, row);
      row = addRow(grid, loc, "field.meleeWeapon", meleeWeapon, row);
      row = addRow(grid, loc, "field.chapterName", chapterName, row);
      addRow(grid, loc, "field.chapterLegion", chapterLegion, row);
      return grid;
    }

    private static int addRow(
        GridPane grid, Localization loc, String key, javafx.scene.Node control, int row) {
      grid.add(new Label(loc.message(key, key)), 0, row);
      grid.add(control, 1, row);
      return row + 1;
    }

    private void fill(SpaceMarine m) {
      name.setText(m.getName());
      coordinateX.setText(String.valueOf(m.getCoordinates().getX()));
      coordinateY.setText(String.valueOf(m.getCoordinates().getY()));
      health.setText(String.valueOf(m.getHealth()));
      height.setText(String.valueOf(m.getHeight()));
      category.setValue(m.getCategory());
      meleeWeapon.setValue(m.getMeleeWeapon());
      if (m.getChapter() != null) {
        chapterName.setText(m.getChapter().getName());
        chapterLegion.setText(m.getChapter().getParentLegion());
      }
    }

    private SpaceMarine toMarine(Localization loc) {
      Coordinates coordinates =
          new Coordinates(
              parseDouble(coordinateX, "field.x", loc), parseLong(coordinateY, "field.y", loc));
      Chapter chapter = readChapter();
      return new SpaceMarine(
          name.getText() == null ? null : name.getText().trim(),
          coordinates,
          (float) parseDouble(health, "field.health", loc),
          parseLong(height, "field.height", loc),
          category.getValue(),
          meleeWeapon.getValue(),
          chapter);
    }

    private Chapter readChapter() {
      String chapter = text(chapterName);
      if (chapter == null) {
        return null;
      }
      return new Chapter(chapter, text(chapterLegion));
    }

    private static String text(TextField field) {
      String value = field.getText();
      return value == null || value.isBlank() ? null : value.trim();
    }

    private static double parseDouble(TextField field, String labelKey, Localization loc) {
      try {
        return Double.parseDouble(field.getText().trim().replace(',', '.'));
      } catch (RuntimeException e) {
        throw new IllegalArgumentException(
            loc.message(
                "error.invalidNumber",
                "Поле «{0}»: введите число",
                loc.message(labelKey, labelKey)));
      }
    }

    private static long parseLong(TextField field, String labelKey, Localization loc) {
      try {
        return Long.parseLong(field.getText().trim());
      } catch (RuntimeException e) {
        throw new IllegalArgumentException(
            loc.message(
                "error.invalidInteger",
                "Поле «{0}»: введите целое число",
                loc.message(labelKey, labelKey)));
      }
    }
  }
}
