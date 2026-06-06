package dev.mikhalexandr.server.ratelimit;

public interface RateLimiter {
  boolean allow(String key);
}
