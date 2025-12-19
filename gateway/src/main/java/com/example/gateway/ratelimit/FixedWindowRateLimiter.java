package com.example.gateway.ratelimit;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter {

  public record Decision(boolean allowed, int remaining, long retryAfterSeconds, long windowResetEpochSeconds) {}

  private static class WindowCounter {
    final long windowStartEpochSeconds;
    final AtomicInteger count = new AtomicInteger(0);

    WindowCounter(long windowStartEpochSeconds) {
      this.windowStartEpochSeconds = windowStartEpochSeconds;
    }
  }

  private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

  public Decision tryConsume(String key, int limit, int windowSeconds) {
    long now = Instant.now().getEpochSecond();
    long windowStart = (now / windowSeconds) * windowSeconds;
    long windowReset = windowStart + windowSeconds;

    String mapKey = key + ":" + windowStart;

    WindowCounter wc = counters.compute(mapKey, (k, existing) -> {
      if (existing == null || existing.windowStartEpochSeconds != windowStart) {
        return new WindowCounter(windowStart);
      }
      return existing;
    });

    int newCount = wc.count.incrementAndGet();
    boolean allowed = newCount <= limit;

    int remaining = Math.max(0, limit - newCount);
    long retryAfter = allowed ? 0 : Math.max(1, windowReset - now);

    return new Decision(allowed, remaining, retryAfter, windowReset);
  }
}
