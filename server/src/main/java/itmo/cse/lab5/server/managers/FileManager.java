package itmo.cse.lab5.server.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import itmo.cse.lab5.common.exceptions.ValidationException;
import itmo.cse.lab5.common.models.SpaceMarine;
import itmo.cse.lab5.server.exceptions.FileReadException;
import itmo.cse.lab5.server.exceptions.FileWriteException;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Выполняет загрузку и сохранение коллекции в JSON-файл.
 */
public class FileManager {
    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());
    private final String filePath;
    private final Gson gson;

    /**
     * @param filePath путь к файлу коллекции
     */
    public FileManager(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().setDateFormat("HH:mm:ss dd.MM.yyyy").create();
    }

    /**
     * Загружает коллекцию из файла, пропуская невалидные элементы.
     *
     * @return загруженная коллекция
     * @throws FileReadException если файл не найден или не удалось прочитать/распарсить
     */
    public LinkedList<SpaceMarine> load() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath))) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || root.isJsonNull()) {
                return new LinkedList<>();
            }

            if (!root.isJsonArray()) {
                throw new FileReadException(String.format("ожидался JSON-массив [%s]", filePath), null);
            }

            return parseValidMarines(root.getAsJsonArray());
        } catch (FileNotFoundException e) {
            throw new FileReadException(String.format("файл не найден [%s]", filePath), e);
        } catch (JsonSyntaxException e) {
            throw new FileReadException(String.format("ошибка парсинга JSON [%s]", filePath), e);
        } catch (IOException e) {
            throw new FileReadException(String.format("ошибка чтения файла [%s]", filePath), e);
        }
    }

    private LinkedList<SpaceMarine> parseValidMarines(JsonArray array) {
        LinkedList<SpaceMarine> result = new LinkedList<>();
        Set<Integer> usedIds = new HashSet<>();

        for (JsonElement element : array) {
            try {
                SpaceMarine marine = gson.fromJson(element, SpaceMarine.class);
                if (marine == null) {
                    LOGGER.warning("Невалидный объект пропущен: элемент равен null");
                    continue;
                }
                marine.validate();
                if (!usedIds.add(marine.getId())) {
                    LOGGER.warning(String.format("Невалидный объект пропущен: дубликат id=%d", marine.getId()));
                    continue;
                }
                result.add(marine);
            } catch (ValidationException | JsonParseException | IllegalStateException e) {
                LOGGER.warning(String.format("Невалидный объект пропущен: %s", e.getMessage()));
            }
        }

        return result;
    }

    /**
     * Сохраняет коллекцию в файл в формате JSON.
     *
     * @param collection коллекция для сохранения
     * @throws FileWriteException если запись в файл не удалась
     */
    public void save(LinkedList<SpaceMarine> collection) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(gson.toJson(collection));
        } catch (IOException e) {
            throw new FileWriteException(String.format("ошибка записи в файл [%s]", filePath), e);
        }
    }
}
