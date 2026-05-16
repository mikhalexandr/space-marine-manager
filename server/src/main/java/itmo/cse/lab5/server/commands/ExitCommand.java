package itmo.cse.lab5.server.commands;

/**
 * Команда {@code exit}: завершает программу без сохранения.
 */
public class ExitCommand extends Command {
    /**
     * Создает команду завершения программы.
     */
    public ExitCommand() {
        super("exit", "завершить программу (без сохранения!)");
    }

    /**
     * Завершает JVM-процесс.
     *
     * @param args аргументы команды (не используются)
     * @return текст результата выполнения
     */
    @Override
    public String execute(String[] args) {
        return "Программа завершена";
    }

    @Override
    public boolean shouldExit() {
        return true;
    }
}
