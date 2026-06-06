package dev.mikhalexandr.server.managers;

import dev.mikhalexandr.common.dto.event.CollectionEvent;
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
  private volatile CollectionEventPublisher eventPublisher = CollectionManager::ignoreEvent;

  public void setEventPublisher(CollectionEventPublisher publisher) {
    this.eventPublisher = publisher == null ? CollectionManager::ignoreEvent : publisher;
  }

  private static void ignoreEvent(CollectionEvent event) {}

  public void loadAll(List<SpaceMarine> marines) {
    lock.writeLock().lock();
    try {
      collection.clear();
      collection.addAll(marines);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void add(SpaceMarine marine) {
    lock.writeLock().lock();
    try {
      collection.add(marine);
    } finally {
      lock.writeLock().unlock();
    }
    eventPublisher.publish(CollectionEvent.added(marine));
  }

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
    eventPublisher.publish(CollectionEvent.updated(newMarine));
  }

  public void removeById(int id) {
    boolean removed;
    lock.writeLock().lock();
    try {
      removed = collection.removeIf(marine -> marine.getId() == id);
    } finally {
      lock.writeLock().unlock();
    }
    if (removed) {
      eventPublisher.publish(CollectionEvent.removed(List.of((long) id)));
    }
  }

  public void removeByOwner(String owner) {
    List<Long> removedIds = new ArrayList<>();
    lock.writeLock().lock();
    try {
      collection.removeIf(
          marine -> {
            if (owner.equals(marine.getOwner())) {
              removedIds.add((long) marine.getId());
              return true;
            }
            return false;
          });
    } finally {
      lock.writeLock().unlock();
    }
    if (!removedIds.isEmpty()) {
      eventPublisher.publish(CollectionEvent.removed(removedIds));
    }
  }

  public SpaceMarine getById(int id) {
    lock.readLock().lock();
    try {
      return findById(id);
    } finally {
      lock.readLock().unlock();
    }
  }

  public SpaceMarine head() {
    lock.readLock().lock();
    try {
      return collection.peekFirst();
    } finally {
      lock.readLock().unlock();
    }
  }

  public boolean isLessThanMin(SpaceMarine candidate) {
    lock.readLock().lock();
    try {
      return collection.isEmpty() || candidate.compareTo(Collections.min(collection)) < 0;
    } finally {
      lock.readLock().unlock();
    }
  }

  public float sumOfHealth() {
    lock.readLock().lock();
    try {
      return (float) collection.stream().mapToDouble(SpaceMarine::getHealth).sum();
    } finally {
      lock.readLock().unlock();
    }
  }

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

  public long countByCategory(AstartesCategory category) {
    lock.readLock().lock();
    try {
      return collection.stream().filter(marine -> marine.getCategory() == category).count();
    } finally {
      lock.readLock().unlock();
    }
  }

  public List<SpaceMarine> snapshot() {
    lock.readLock().lock();
    try {
      return new ArrayList<>(collection);
    } finally {
      lock.readLock().unlock();
    }
  }

  public int size() {
    lock.readLock().lock();
    try {
      return collection.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  public String getType() {
    return collection.getClass().getSimpleName();
  }

  public String getInitializationDateFormatted() {
    return initializationDate.toInstant().atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
  }

  private SpaceMarine findById(int id) {
    return collection.stream().filter(marine -> marine.getId() == id).findFirst().orElse(null);
  }
}
