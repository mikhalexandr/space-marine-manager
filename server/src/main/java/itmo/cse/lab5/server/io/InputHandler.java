package itmo.cse.lab5.server.io;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.models.*;
import itmo.cse.lab5.common.util.Validator;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Обрабатывает интерактивный ввод сущности {@link SpaceMarine} из консоли.
 */
public class InputHandler {
    private Scanner scanner;

    /**
     * @param scanner источник пользовательского ввода
     */
    public InputHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * @return текущий источник ввода
     */
    public Scanner getScanner() {
        return scanner;
    }

    /**
     * Меняет источник ввода (используется при выполнении скриптов).
     *
     * @param scanner новый источник ввода
     */
    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Читает все поля SpaceMarine с валидацией.
     *
     * @return валидный объект SpaceMarine
     */
    public SpaceMarine readSpaceMarine() throws CommandExecutionException {
        String name = readName(null);
        Coordinates coordinates = readCoordinates(null);
        float health = readHealth(null);
        long height = readHeight(null);
        AstartesCategory category = readCategory(null);
        MeleeWeapon meleeWeapon = readMeleeWeapon(null);
        Chapter chapter = readChapter(null);
        return new SpaceMarine(
                name, coordinates, health, height, category, meleeWeapon,  chapter
        );
    }

    /**
     * Читает все поля SpaceMarine для обновления, показывая текущие значения.
     *
     * @param current текущее значение элемента
     * @return валидный объект SpaceMarine
     */
    public SpaceMarine readSpaceMarine(SpaceMarine current) throws CommandExecutionException {
        String name = readName(current.getName());
        Coordinates coordinates = readCoordinates(current.getCoordinates());
        float health = readHealth(current.getHealth());
        long height = readHeight(current.getHeight());
        AstartesCategory category = readCategory(current.getCategory());
        MeleeWeapon meleeWeapon = readMeleeWeapon(current.getMeleeWeapon());
        Chapter chapter = readChapter(current.getChapter());
        return new SpaceMarine(
                name, coordinates, health, height, category, meleeWeapon,  chapter
        );
    }

    private String readName(String currentName) throws CommandExecutionException {
        while (true) {
            System.out.print(prompt("Введите имя", currentName));
            String input = nextLineOrThrow().trim();
            if (Validator.isValidString(input)) {
                return input;
            }
            System.err.println("Ошибка: имя не может быть пустым");
        }
    }

    private Coordinates readCoordinates(Coordinates currentCoordinates) throws CommandExecutionException {
        Double currentX = currentCoordinates == null ? null : currentCoordinates.getX();
        Long currentY = currentCoordinates == null ? null : currentCoordinates.getY();
        double x = readDouble(currentX);
        long y = readLong("Введите координату y", currentY);
        return new Coordinates(x, y);
    }

    private double readDouble(Number currentValue) throws CommandExecutionException {
        while (true) {
            System.out.print(prompt("Введите координату x", currentValue));
            try {
                return Double.parseDouble(nextLineOrThrow().trim());
            } catch (NumberFormatException e) {
                System.err.println("Ошибка: введите число");
            }
        }
    }

    private long readLong(String label, Number currentValue) throws CommandExecutionException {
        while (true) {
            System.out.print(prompt(label, currentValue));
            try {
                return Long.parseLong(nextLineOrThrow().trim());
            } catch (NumberFormatException e) {
                System.err.println("Ошибка: введите целое число");
            }
        }
    }

    private float readHealth(Float currentHealth) throws CommandExecutionException {
        while (true) {
            System.out.print(prompt("Введите здоровье (> 0)", currentHealth));
            try {
                float health = Float.parseFloat(nextLineOrThrow().trim());
                if (Validator.isGreaterThan(health, 0)) {
                    return health;
                }
                System.err.println("Ошибка: здоровье должно быть больше 0");
            } catch (NumberFormatException e) {
                System.err.println("Ошибка: введите число");
            }
        }
    }

    private long readHeight(Long currentHeight) throws CommandExecutionException {
        return readLong("Введите рост", currentHeight);
    }

    private AstartesCategory readCategory(AstartesCategory currentCategory) throws CommandExecutionException {
        AstartesCategory[] values = AstartesCategory.values();
        while (true) {
            String current = currentCategory == null ? "-" : currentCategory.name();
            System.out.println(currentCategory == null
                    ? "Выберите категорию:"
                    : "Выберите категорию [сейчас: " + current + "]:");
            for (int i = 0; i < values.length; i++) {
                System.out.printf("  %d - %s%n", i + 1, values[i]);
            }
            System.out.print("Введите номер: ");
            String input = nextLineOrThrow().trim();
            if (input.isEmpty()) {
                System.err.println("Ошибка: значение не может быть пустым");
                continue;
            }
            int index = parseEnumIndex(input, values.length);
            if (index != -1) {
                return values[index];
            }
        }
    }

    private MeleeWeapon readMeleeWeapon(MeleeWeapon currentMeleeWeapon) throws CommandExecutionException {
        MeleeWeapon[] values = MeleeWeapon.values();
        while (true) {
            String current = currentMeleeWeapon == null ? "-" : currentMeleeWeapon.name();
            System.out.println(currentMeleeWeapon == null
                    ? "Выберите оружие ближнего боя (Enter - пропустить):"
                    : "Выберите оружие ближнего боя [сейчас: " + current + "] (Enter - пропустить):");
            for (int i = 0; i < values.length; i++) {
                System.out.printf("  %d - %s%n", i + 1, values[i]);
            }
            System.out.print("Введите номер: ");
            String input = nextLineOrThrow().trim();
            if (input.isEmpty()) {
                return null;
            }
            int index = parseEnumIndex(input, values.length);
            if (index != -1) {
                return values[index];
            }
        }
    }

    private Chapter readChapter(Chapter currentChapter) throws CommandExecutionException {
        String currentName = currentChapter == null ? null : currentChapter.getName();
        String currentLegion = currentChapter == null ? null : currentChapter.getParentLegion();

        System.out.print(prompt("Введите имя ордена (Enter - пропустить)", currentName));
        String name = nextLineOrThrow().trim();
        if (name.isEmpty()) {
            return null;
        }

        System.out.print(prompt("Введите родительский легион (Enter - пропустить)", currentLegion));
        String legion = nextLineOrThrow().trim();
        return new Chapter(name, legion.isEmpty() ? null : legion);
    }

    private String nextLineOrThrow() throws CommandExecutionException {
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            throw new CommandExecutionException("Недостаточно данных для чтения полей объекта");
        }
    }

    private int parseEnumIndex(String input, int valuesLength) {
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < valuesLength) {
                return index;
            }
            System.err.printf("Ошибка: введите номер от 1 до %d%n", valuesLength);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: введите число");
        }
        return -1;
    }

    private String prompt(String label, Object currentValue) {
        if (currentValue == null) {
            return label + ": ";
        }
        return label + " [сейчас: " + currentValue + "]: ";
    }
}
