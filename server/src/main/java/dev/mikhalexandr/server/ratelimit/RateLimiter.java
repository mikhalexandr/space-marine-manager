package dev.mikhalexandr.server.ratelimit;

/** Контракт лимитера запросов */
public interface RateLimiter {
  /**
   * @param key идентификатор клиента
   * @return тру, если запрос в пределах лимита, а фолс, если лимит превышен
   */
  boolean allow(String key);
}
