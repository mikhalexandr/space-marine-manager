package dev.mikhalexandr.server.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/** Лимитер на Redis */
public final class RedisRateLimiter implements RateLimiter, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimiter.class);
  private static final String KEY_PREFIX = "ratelimit:";

  private final JedisPool pool;
  private final int maxRequests;
  private final long windowMillis;

  public RedisRateLimiter(JedisPool pool, int maxRequests, long windowMillis) {
    this.pool = pool;
    this.maxRequests = maxRequests;
    this.windowMillis = windowMillis;
  }

  @Override
  public boolean allow(String key) {
    String redisKey = KEY_PREFIX + key;
    try (Jedis jedis = pool.getResource()) {
      long count = jedis.incr(redisKey);
      if (count == 1) {
        jedis.pexpire(redisKey, windowMillis);
      }
      return count <= maxRequests;
    } catch (RuntimeException e) {
      LOGGER.warn("Редис не на связи, пропуск запроса: {}", e.getMessage());
      return true;
    }
  }

  @Override
  public void close() {
    pool.close();
  }
}
