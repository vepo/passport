package dev.vepo.passport.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthRateLimiter {

    private record Window(Instant startedAt, AtomicInteger count) {}

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String clientKey, int maxRequests, Duration windowSize) {
        var now = Instant.now();
        var window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || Duration.between(existing.startedAt(), now).compareTo(windowSize) >= 0) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= maxRequests;
    }

}
