package itmo.cse.lab5.server.managers;

import itmo.cse.lab5.common.models.AstartesCategory;
import itmo.cse.lab5.common.models.SpaceMarine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Управляет коллекцией {@link SpaceMarine} и операциями над ней.
 */
public class CollectionManager {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy");

    private LinkedList<SpaceMarine> collection = new LinkedList<>();
    private final Date intializationDate = new Date();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private int generateId() {
        return nextId.getAndIncrement();
    }

    /**
     * Добавляет элемент в коллекцию с автоматически назначенными id и датой создания.
     *
     * @param spaceMarine добавляемый объект
     */
    public void add(SpaceMarine spaceMarine) {
        spaceMarine.setId(generateId());
        spaceMarine.setCreationDate(new Date());
        collection.add(spaceMarine);
    }

    /**
     * Заменяет элемент по id, сохраняя id и дату создания.
     *
     * @param id идентификатор существующего элемента
     * @param newSpaceMarine новое значение
     * @throws NoSuchElementException если элемент с таким id отсутствует
     */
    public void update(int id, SpaceMarine newSpaceMarine) {
        SpaceMarine old = getById(id);
        if (old == null) {
            throw new NoSuchElementException(
                    String.format("SpaceMarine с id %d не найден", id)
            );
        }
        newSpaceMarine.setId(id);
        newSpaceMarine.setCreationDate(old.getCreationDate());
        collection.set(collection.indexOf(old), newSpaceMarine);
    }

    /**
     * @param id идентификатор элемента
     * @return найденный элемент или null
     */
    public SpaceMarine getById(int id) {
        return collection.stream()
                .filter(x -> x.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Удаляет элемент по идентификатору.
     *
     * @param id идентификатор элемента
     * @return true, если элемент был удален
     */
    public boolean removeById(int id) {
        return collection.removeIf(x -> x.getId() == id);
    }

    /**
     * Очищает коллекцию.
     */
    public void clear() {
        collection.clear();
    }

    /**
     * @return первый элемент коллекции или null, если коллекция пуста
     */
    public SpaceMarine head() {
        return collection.peekFirst();
    }

    /**
     * Добавляет элемент только если он меньше минимального в коллекции.
     *
     * @param spaceMarine кандидат на добавление
     * @return true, если элемент добавлен
     */
    public boolean addIfMin(SpaceMarine spaceMarine) {
        if (collection.isEmpty() || spaceMarine.compareTo(Collections.min(collection)) < 0) {
            add(spaceMarine);
            return true;
        }
        return false;
    }

    /**
     * @return сумма поля health по всем элементам
     */
    public float sumOfHealth() {
        return (float) collection.stream()
                .mapToDouble(SpaceMarine::getHealth)
                .sum();
    }

    /**
     * @return элемент с максимальным chapter или null, если подходящих нет
     */
    public SpaceMarine maxByChapter() {
        return collection.stream()
                .filter(x -> x.getChapter() != null)
                .max(Comparator.comparing(SpaceMarine::getChapter))
                .orElse(null);
    }

    /**
     * Подсчитывает количество элементов заданной категории.
     *
     * @param category категория для фильтрации
     * @return число элементов этой категории
     */
    public long countByCategory(AstartesCategory category) {
        return collection.stream()
                .filter(x -> x.getCategory() == category)
                .count();
    }

    /**
     * @return текущая коллекция
     */
    public LinkedList<SpaceMarine> getCollection() {
        return collection;
    }

    /**
     * Устанавливает коллекцию и синхронизирует генератор id.
     *
     * @param loaded загруженная коллекция
     */
    public void setCollection(LinkedList<SpaceMarine> loaded) {
        this.collection = loaded;
        int maxId = collection.stream()
                .mapToInt(SpaceMarine::getId)
                .max()
                .orElse(0);
        nextId.set(maxId + 1);
    }

    /**
     * @return количество элементов в коллекции
     */
    public int size() {
        return collection.size();
    }

    /**
     * @return тип внутренней структуры коллекции
     */
    public String getType() {
        return collection.getClass().getSimpleName();
    }

    /**
     * @return дата инициализации менеджера в формате HH:mm:ss dd.MM.yyyy
     */
    public String getInitializationDateFormatted() {
            return intializationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_FORMATTER);
    }
}
