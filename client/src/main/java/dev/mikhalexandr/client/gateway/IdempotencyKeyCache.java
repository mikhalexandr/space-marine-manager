package dev.mikhalexandr.client.gateway;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class IdempotencyKeyCache {
  private static final int DEFAULT_MAX_ENTRIES = 256;

  private final Map<String, String> keys;

  public IdempotencyKeyCache() {
    this(DEFAULT_MAX_ENTRIES);
  }

  public IdempotencyKeyCache(int maxEntries) {
    this.keys = Collections.synchronizedMap(new LruMap(maxEntries));
  }

  public String requestIdFor(String operationKey) {
    return keys.computeIfAbsent(operationKey, key -> UUID.randomUUID().toString());
  }

  public void resolve(String operationKey) {
    keys.remove(operationKey);
  }

  private static final class LruMap extends LinkedHashMap<String, String> {
    @Serial private static final long serialVersionUID = 1L;
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private final int maxEntries;

    LruMap(int maxEntries) {
      super(INITIAL_CAPACITY, LOAD_FACTOR, true);
      this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
      return size() > maxEntries;
    }
  }
}
