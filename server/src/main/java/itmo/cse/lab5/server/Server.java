package itmo.cse.lab5.server;

import itmo.cse.lab5.common.util.Validator;
import itmo.cse.lab5.server.app.ApplicationBootstrap;
import itmo.cse.lab5.server.app.ApplicationContext;
import itmo.cse.lab5.server.app.ApplicationRunner;
import itmo.cse.lab5.server.logging.LoggingConfigurator;

import java.util.Scanner;

/**
 * Точка входа серверного приложения.
 */
public final class Server {

    private Server() {
        throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
    }

    /**
     * Запускает интерактивный цикл командного интерпретатора.
     *
     * @param args первый аргумент должен содержать путь к JSON-файлу коллекции
     */
    public static void main(String[] args) {
        if (args.length == 0 || !Validator.isValidString(args[0])) {
            System.err.println("Ошибка инициализации сервиса\nФормат запуска: java -jar [SERVER_JAR] <путь к файлу>");
            System.exit(1);
        }

        LoggingConfigurator.configureFileLogging();

        try (Scanner scanner = new Scanner(System.in)) {
            ApplicationBootstrap bootstrap = new ApplicationBootstrap();
            ApplicationContext context = bootstrap.bootstrap(args[0], scanner);
            ApplicationRunner applicationRunner = new ApplicationRunner(context);
            applicationRunner.run();
        }
    }
}
