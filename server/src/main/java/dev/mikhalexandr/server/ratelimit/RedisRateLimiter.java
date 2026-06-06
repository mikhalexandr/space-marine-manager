package dev.mikhalexandr.server.ratelimit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public final class RedisRateLimiter implements RateLimiter, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimiter.class);
  private static final String KEY_PREFIX = "ratelimit:";
  private static final String INCREMENT_SCRIPT_PATH = "/redis/increment-rate-limit.lua";

  private final JedisPool pool;
  private final int maxRequests;
  private final long windowMillis;
  private final String incrementScript;

  public RedisRateLimiter(JedisPool pool, int maxRequests, long windowMillis) {
    this.pool = pool;
    this.maxRequests = maxRequests;
    this.windowMillis = windowMillis;
    this.incrementScript = loadIncrementScript();
  }

  @Override
  public boolean allow(String key) {
    String redisKey = KEY_PREFIX + key;
    try (Jedis jedis = pool.getResource()) {
      long count =
          ((Number)
                  jedis.eval(
                      incrementScript, List.of(redisKey), List.of(Long.toString(windowMillis))))
              .longValue();
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

  private static String loadIncrementScript() {
    try (InputStream input = RedisRateLimiter.class.getResourceAsStream(INCREMENT_SCRIPT_PATH)) {
      if (input == null) {
        throw new IllegalStateException("Не найден Lua-скриптик " + INCREMENT_SCRIPT_PATH);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Не прокнуло загрузить Lua-скриптик " + INCREMENT_SCRIPT_PATH, e);
    }
  }
}
