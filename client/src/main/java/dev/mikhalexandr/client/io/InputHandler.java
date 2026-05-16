package dev.mikhalexandr.client.io;

import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.Chapter;
import dev.mikhalexandr.common.models.Coordinates;
import dev.mikhalexandr.common.models.MeleeWeapon;
import dev.mikhalexandr.common.models.SpaceMarine;
import dev.mikhalexandr.common.util.Validator;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Читает поля SpaceMarine из консоли на стороне клиента. */
public class InputHandler {
  private static final String PROMPT_PREFIX = "| ";

  /**
   * @param scanner источник пользовательского ввода
   * @param showPrompts показывать ли подсказки в процессе ввода
   * @return валидный объект SpaceMarine
   */
  public SpaceMarine read(Scanner scanner, boolean showPrompts) {
    String name = readName(scanner, showPrompts);
    double x = readDouble(scanner, showPrompts);
    long y = readLong(scanner, "Введите координату y", showPrompts);
    float health = readHealth(scanner, showPrompts);
    long height = readLong(scanner, "Введите рост", showPrompts);
    AstartesCategory category = readCategory(scanner, showPrompts);
    MeleeWeapon meleeWeapon = readMeleeWeapon(scanner, showPrompts);
    Chapter chapter = readChapter(scanner, showPrompts);
    return new SpaceMarine(
        name, new Coordinates(x, y), health, height, category, meleeWeapon, chapter);
  }

  private static String readName(Scanner scanner, boolean showPrompts) {
    while (true) {
      printPrompt("Введите имя: ", showPrompts);
      String input = nextLineOrThrow(scanner, "имя").trim();
      if (Validator.isValidString(input)) {
        return input;
      }
      System.out.println("| Ошибка: имя не может быть пустым");
    }
  }

  private static double readDouble(Scanner scanner, boolean showPrompts) {
    while (true) {
      printPrompt("Введите координату x: ", showPrompts);
      try {
        return Double.parseDouble(nextLineOrThrow(scanner, "координата x").trim());
      } catch (NumberFormatException e) {
        System.out.println("| Ошибка: введите число");
      }
    }
  }

  private static long readLong(Scanner scanner, String prompt, boolean showPrompts) {
    while (true) {
      printPrompt(prompt + ": ", showPrompts);
      try {
        return Long.parseLong(nextLineOrThrow(scanner, prompt).trim());
      } catch (NumberFormatException e) {
        System.out.println("| Ошибка: введите целое число");
      }
    }
  }

  private static float readHealth(Scanner scanner, boolean showPrompts) {
    while (true) {
      printPrompt("Введите здоровье (> 0): ", showPrompts);
      try {
        float value = Float.parseFloat(nextLineOrThrow(scanner, "здоровье").trim());
        if (Validator.isGreaterThan(value, 0)) {
          return value;
        }
      } catch (NumberFormatException ignored) {
        // Сообщение об ошибке печатается ниже
      }
      System.out.println("| Ошибка: здоровье должно быть числом больше 0");
    }
  }

  private static AstartesCategory readCategory(Scanner scanner, boolean showPrompts) {
    AstartesCategory[] values = AstartesCategory.values();
    while (true) {
      printLine("Выберите категорию:", showPrompts);
      printEnumValues(values, showPrompts);
      int index = readEnumIndex(scanner, values.length, showPrompts);
      if (index >= 0) {
        return values[index];
      }
    }
  }

  private static MeleeWeapon readMeleeWeapon(Scanner scanner, boolean showPrompts) {
    MeleeWeapon[] values = MeleeWeapon.values();
    while (true) {
      printLine("Выберите оружие ближнего боя (Enter - пропустить):", showPrompts);
      printEnumValues(values, showPrompts);
      printPrompt("Введите номер: ", showPrompts);
      String input = nextLineOrThrow(scanner, "оружие ближнего боя").trim();
      if (input.isEmpty()) {
        return null;
      }
      try {
        int index = Integer.parseInt(input) - 1;
        if (index >= 0 && index < values.length) {
          return values[index];
        }
      } catch (NumberFormatException ignored) {
        // Сообщение об ошибке печатается ниже
      }
      System.out.println("| Ошибка: введите корректный номер");
    }
  }

  private static Chapter readChapter(Scanner scanner, boolean showPrompts) {
    printPrompt("Введите имя ордена (Enter - пропустить): ", showPrompts);
    String chapterName = nextLineOrThrow(scanner, "имя ордена").trim();
    if (chapterName.isEmpty()) {
      return null;
    }

    printPrompt("Введите родительский легион (Enter - пропустить): ", showPrompts);
    String legion = nextLineOrThrow(scanner, "родительский легион").trim();
    return new Chapter(chapterName, legion.isEmpty() ? null : legion);
  }

  private static <T extends Enum<T>> void printEnumValues(T[] values, boolean showPrompts) {
    if (!showPrompts) {
      return;
    }
    for (int i = 0; i < values.length; i++) {
      System.out.printf("|   %d - %s%n", i + 1, values[i]);
    }
  }

  private static int readEnumIndex(Scanner scanner, int length, boolean showPrompts) {
    printPrompt("Введите номер: ", showPrompts);
    try {
      int index = Integer.parseInt(nextLineOrThrow(scanner, "номер").trim()) - 1;
      if (index >= 0 && index < length) {
        return index;
      }
    } catch (NumberFormatException ignored) {
      // Сообщение об ошибке печатается ниже
    }
    System.out.printf("| Ошибка: введите номер от 1 до %d%n", length);
    return -1;
  }

  private static void printPrompt(String text, boolean showPrompts) {
    if (!showPrompts) {
      return;
    }
    System.out.print(PROMPT_PREFIX + text);
  }

  private static void printLine(String text, boolean showPrompts) {
    if (!showPrompts) {
      return;
    }
    System.out.println(PROMPT_PREFIX + text);
  }

  private static String nextLineOrThrow(Scanner scanner, String fieldName) {
    try {
      return scanner.nextLine();
    } catch (NoSuchElementException e) {
      throw new InputReadException(
          String.format(
              "Недостаточно данных для заполнения SpaceMarine (ожидается поле: %s)", fieldName));
    }
  }

  /** Ошибка чтения полей SpaceMarine из источника ввода. */
  public static class InputReadException extends IllegalStateException {
    public InputReadException(String message) {
      super(message);
    }
  }
}
