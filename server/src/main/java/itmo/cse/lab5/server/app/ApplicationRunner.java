package itmo.cse.lab5.server.app;

import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.dto.response.CommandResponse;

/**
 * Основной цикл чтения, выполнения и вывода команд.
 */
public class ApplicationRunner {
    private final ApplicationContext context;

    /**
     * @param context контекст зависимостей выполнения
     */
    public ApplicationRunner(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Запускает интерактивный цикл обработки команд.
     */
    public void run() {
        context.getCommandOutput().printMessage("Введите help для списка команд.");
        boolean running = true;
        while (running) {
            context.getCommandOutput().printPrompt();
            String rawCommand = context.getCommandReader().readRawCommand();
            if (rawCommand == null) {
                running = false;
                continue;
            }

            CommandRequest request = context.getCommandRequestParser().toRequest(rawCommand);
            if (request == null) {
                continue;
            }

            CommandResponse response = context.getCommandExecutor().execute(request);
            context.getCommandOutput().printResponse(response);
            running = !response.isExitRequested();
        }
    }
}
