package itmo.cse.lab5.server.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * Настраивает файловое логирование серверного приложения.
 */
public final class LoggingConfigurator {
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd:MM:yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private LoggingConfigurator() {
        throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
    }

    /**
     * Включает запись логов в файл и отключает вывод логов в консоль.
     */
    public static void configureFileLogging() {
        try {
            Path logDirectory = Path.of(LOG_DIRECTORY);
            Files.createDirectories(logDirectory);

            Logger rootLogger = LogManager.getLogManager().getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            rootLogger.addHandler(createFileHandler(logDirectory));
            rootLogger.addHandler(createConsoleHandler());

            rootLogger.setLevel(Level.INFO);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось настроить файловое логирование", e);
        }
    }

    private static FileHandler createFileHandler(Path logDirectory) throws IOException {
        FileHandler fileHandler = new FileHandler(logDirectory.resolve(LOG_FILE).toString(), true);
        fileHandler.setEncoding("UTF-8");
        fileHandler.setLevel(Level.INFO);
        fileHandler.setFormatter(new PrettyLogFormatter());
        return fileHandler;
    }

    private static ConsoleHandler createConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.WARNING);
        consoleHandler.setFormatter(new PrettyLogFormatter());
        return consoleHandler;
    }

    private static final class PrettyLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String timestamp = TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
            String loggerName = record.getLoggerName();
            String shortLoggerName = loggerName == null
                    ? "root"
                    : loggerName.substring(loggerName.lastIndexOf('.') + 1);

            return String.format(
                    "[%s] %-7s [%s] %s%n",
                    timestamp,
                    record.getLevel().getName(),
                    shortLoggerName,
                    formatMessage(record)
            );
        }
    }
}
