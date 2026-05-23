package dev.mikhalexandr.server.managers;

import dev.mikhalexandr.common.models.AstartesCategory;
import dev.mikhalexandr.common.models.SpaceMarine;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CollectionManager {
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy");

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final LinkedList<SpaceMarine> collection = new LinkedList<>();
  private final Date initializationDate = new Date();

  /**
   * Полностью заменяет коллекцию в памяти
   *
   * @param marines загруженные из бд элементы
   */
  public void loadAll(List<SpaceMarine> marines) {
    lock.writeLock().lock();
    try {
      collection.clear();
      collection.addAll(marines);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Добавляет элемент в коллекцию в памяти. Вызывается только после успешной вставки в бд
   *
   * @param marine элемент с уже проставленными id и датой создания
   */
  public void add(SpaceMarine marine) {
    lock.writeLock().lock();
    try {
      collection.add(marine);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Заменяет элемент по id, сохраняя id, дату создания и владельца
   *
   * @param id идентификатор существующего элемента
   * @param newMarine новое значение
   */
  public void update(int id, SpaceMarine newMarine) {
    lock.writeLock().lock();
    try {
      SpaceMarine old = findById(id);
      if (old == null) {
        throw new NoSuchElementException(String.format("SpaceMarine с id %d не найден", id));
      }
      newMarine.setId(id);
      newMarine.setCreationDate(old.getCreationDate());
      newMarine.setOwner(old.getOwner());
      collection.set(collection.indexOf(old), newMarine);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Удаляет элемент по идентификатору
   *
   * @param id идентификатор элемента
   */
  public void removeById(int id) {
    lock.writeLock().lock();
    try {
      collection.removeIf(marine -> marine.getId() == id);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Удаляет все элементы указанного владельца.
   *
   * @param owner логин пользователя-владельца
   */
  public void removeByOwner(String owner) {
    lock.writeLock().lock();
    try {
        collection.removeIf(marine -> owner.equals(marine.getOwner()));
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * @param id идентификатор элемента
   * @return найденный элемент или null
   */
  public SpaceMarine getById(int id) {
    lock.readLock().lock();
    try {
      return findById(id);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return первый элемент коллекции или null, если коллекция пуста
   */
  public SpaceMarine head() {
    lock.readLock().lock();
    try {
      return collection.peekFirst();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @param candidate кандидат на добавление
   * @return true, если коллекция пуста или кандидат меньше минимального элемента
   */
  public boolean isLessThanMin(SpaceMarine candidate) {
    lock.readLock().lock();
    try {
      return collection.isEmpty() || candidate.compareTo(Collections.min(collection)) < 0;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return сумма поля health по всем элементам
   */
  public float sumOfHealth() {
    lock.readLock().lock();
    try {
      return (float) collection.stream().mapToDouble(SpaceMarine::getHealth).sum();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return элемент с максимальным chapter или null, если подходящих нет
   */
  public SpaceMarine maxByChapter() {
    lock.readLock().lock();
    try {
      return collection.stream()
          .filter(marine -> marine.getChapter() != null)
          .max(Comparator.comparing(SpaceMarine::getChapter))
          .orElse(null);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @param category категория для фильтрации
   * @return число элементов этой категории
   */
  public long countByCategory(AstartesCategory category) {
    lock.readLock().lock();
    try {
      return collection.stream().filter(marine -> marine.getCategory() == category).count();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return копия коллекции на момент вызова
   */
  public List<SpaceMarine> snapshot() {
    lock.readLock().lock();
    try {
      return new ArrayList<>(collection);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return количество элементов в коллекции
   */
  public int size() {
    lock.readLock().lock();
    try {
      return collection.size();
    } finally {
      lock.readLock().unlock();
    }
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
    return initializationDate.toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
  }

  private SpaceMarine findById(int id) {
    return collection.stream().filter(marine -> marine.getId() == id).findFirst().orElse(null);
  }
}
