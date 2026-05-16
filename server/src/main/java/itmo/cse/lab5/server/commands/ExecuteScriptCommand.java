package itmo.cse.lab5.server.commands;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.dto.response.CommandResponse;
import itmo.cse.lab5.server.io.CommandRequestParser;
import itmo.cse.lab5.server.io.InputHandler;
import itmo.cse.lab5.server.managers.CommandExecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Команда {@code execute_script}: выполняет команды из указанного файла.
 */
public class ExecuteScriptCommand extends Command {
    private static final int MAX_RECURSION_DEPTH = 3;
    private static int recursionDepth;
    private final CommandExecutor commandExecutor;
    private final InputHandler inputHandler;
    private final CommandRequestParser commandRequestParser;
    private boolean exitRequested;

    /**
     * @param commandExecutor исполнитель команд
     * @param inputHandler обработчик пользовательского ввода
     * @param commandRequestParser парсер строковых команд
     */
    public ExecuteScriptCommand(CommandExecutor commandExecutor,
                                InputHandler inputHandler,
                                CommandRequestParser commandRequestParser) {
        super("execute_script", "<file_name>", "выполнить скрипт из файла");
        this.commandExecutor = commandExecutor;
        this.inputHandler = inputHandler;
        this.commandRequestParser = commandRequestParser;
    }

    /**
     * Считывает файл скрипта и выполняет команды построчно.
     *
     * @param args ожидается один аргумент: имя файла скрипта
     * @return лог выполнения скрипта
     * @throws CommandExecutionException если аргумент не передан или файл не найден
     */
    @Override
    public String execute(String[] args) throws CommandExecutionException {
        exitRequested = false;
        String fileName = parseFileName(args);
        int nextDepth = enterRecursionLevel();
        if (nextDepth >= MAX_RECURSION_DEPTH) {
            throw new CommandExecutionException(
                    String.format(
                            "Достигнута максимальная глубина рекурсии (%d) при вызове скрипта: %s",
                            MAX_RECURSION_DEPTH,
                            fileName
                    )
            );
        }

        try {
            return executeScriptLines(fileName);
        } finally {
            exitRecursionLevel();
        }
    }

    @Override
    public boolean shouldExit() {
        return exitRequested;
    }

    private String parseFileName(String[] args) throws CommandExecutionException {
        if (args.length == 0) {
            throw new CommandExecutionException("Использование: execute_script <file_name>");
        }
        String[] fileNameArgs = args[0].trim().split("\\s+");
        if (fileNameArgs.length != 1) {
            throw new CommandExecutionException("Формат: execute_script <file_name>");
        }
        return fileNameArgs[0];
    }

    private static synchronized int enterRecursionLevel() {
        recursionDepth++;
        return recursionDepth;
    }

    private static synchronized void exitRecursionLevel() {
        recursionDepth--;
        if (recursionDepth < 0) {
            recursionDepth = 0;
        }
    }

    private String executeScriptLines(String fileName) throws CommandExecutionException {
        StringBuilder builder = new StringBuilder();
        try (Scanner fileScanner = new Scanner(new File(fileName))) {
            Scanner previousScanner = inputHandler.getScanner();
            try {
                inputHandler.setScanner(fileScanner);
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine();
                    if (!line.isBlank()) {
                        builder.append("$ ").append(line).append(System.lineSeparator());
                        CommandRequest request = commandRequestParser.toRequest(line);
                        if (request == null) {
                            continue;
                        }
                        CommandResponse response = commandExecutor.execute(request);
                        if (response.getMessage() != null && !response.getMessage().isBlank()) {
                            builder.append(response.getMessage()).append(System.lineSeparator());
                        }
                        if (response.isExitRequested()) {
                            exitRequested = true;
                            break;
                        }
                    }
                }
            } finally {
                inputHandler.setScanner(previousScanner);
            }
        } catch (FileNotFoundException e) {
            throw new CommandExecutionException(String.format("файл [%s] не найден", fileName));
        }
        return builder.toString().trim();
    }
}
