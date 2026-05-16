package itmo.cse.lab5.server.io;

import java.util.Scanner;

/**
 * Читает сырые строки команд из источника ввода.
 */
public class CommandReader {
    private final Scanner scanner;

    /**
     * @param scanner источник ввода команд
     */
    public CommandReader(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * @return следующая строка команды или {@code null}, если ввод завершен
     */
    public String readRawCommand() {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }
}
