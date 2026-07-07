package dev.vepo.contraponto.activitypub;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubFetchRateLimiter {

    private final ActivityPubFetchSettings settings;
    private final ConcurrentHashMap<String, Deque<Instant>> fetchesByDomain = new ConcurrentHashMap<>();

    @Inject
    public ActivityPubFetchRateLimiter(ActivityPubFetchSettings settings) {
        this.settings = settings;
    }

    public boolean tryAcquire(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }
        var normalized = domain.toLowerCase(Locale.ROOT);
        var windowStart = Instant.now().minusSeconds(60);
        var timestamps = fetchesByDomain.computeIfAbsent(normalized, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= settings.maxPerDomainPerMinute()) {
                return false;
            }
            timestamps.addLast(Instant.now());
            return true;
        }
    }
}
