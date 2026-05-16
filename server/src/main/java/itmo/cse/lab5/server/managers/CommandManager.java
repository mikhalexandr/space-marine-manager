package itmo.cse.lab5.server.managers;

import itmo.cse.lab5.server.exceptions.CommandExecutionException;
import itmo.cse.lab5.common.dto.request.CommandRequest;
import itmo.cse.lab5.common.dto.response.CommandResponse;
import itmo.cse.lab5.server.commands.CommandContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранит зарегистрированные команды и исполняет их по строковому вводу.
 */
public class CommandManager implements CommandExecutor {
    private static final int HISTORY_SIZE = 5;
    private final Map<String, CommandContract> commands = new LinkedHashMap<>();
    private final List<String> history = new ArrayList<>();

    /**
     * Регистрирует новую команду.
     *
     * @param command экземпляр команды
     */
    public void register(CommandContract command) {
        if (commands.containsKey(command.getName())) {
            throw new IllegalStateException(String.format("Команда '%s' уже зарегистрирована", command.getName()));
        }
        commands.put(command.getName(), command);
    }

    /**
     * Регистрирует карту команд.
     *
     * @param commandMap карта команд
     */
    public void registerAll(Map<String, CommandContract> commandMap) {
        for (Map.Entry<String, CommandContract> entry : commandMap.entrySet()) {
            register(entry.getValue());
        }
    }

    /**
     * Выполняет команду из запроса и возвращает ответ.
     *
     * @param request запрос на выполнение команды
     * @return результат выполнения
     */
    @Override
    public CommandResponse execute(CommandRequest request) {
        if (request == null || request.getCommandName() == null || request.getCommandName().isBlank()) {
            return CommandResponse.failure("Пустая команда");
        }

        String name = request.getCommandName().toLowerCase();
        String[] args = request.getCommandArguments();

        CommandContract command = commands.get(name);
        if (command == null) {
            return CommandResponse.failure(
                    String.format("Неизвестная команда: %s. Введите help для отображения списка команд", name)
            );
        }

        try {
            String result = command.execute(args);
            addToHistory(name, command.getArgs());
            return CommandResponse.success(result, command.shouldExit());
        } catch (CommandExecutionException e) {
            return CommandResponse.failure(String.format("Ошибка выполнения команды: %s", e.getMessage()));
        }
    }

    /**
     * Добавляет название команды в историю с ограничением по размеру.
     *
     * @param name имя выполненной команды
     */
    public void addToHistory(String name, String args) {
        String info = name + " " + args;
        history.add(info);
        if (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }
    }

    /**
     * @return список недавно выполненных команд
     */
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * @return карта зарегистрированных команд
     */
    public Map<String, CommandContract> getCommands() {
        return Collections.unmodifiableMap(commands);
    }
}
