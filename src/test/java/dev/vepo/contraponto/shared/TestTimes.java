package dev.vepo.contraponto.shared;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed timestamps for deterministic tests. Do not use
 * {@code LocalDateTime.now()}, {@code YearMonth.now()}, or similar system-clock
 * calls in tests — use these constants instead.
 */
public final class TestTimes {

    public static final LocalDateTime REFERENCE = LocalDateTime.of(2026, 5, 15, 12, 0);

    public static final YearMonth REFERENCE_MONTH = YearMonth.from(REFERENCE);

    public static final LocalDate REFERENCE_DATE = REFERENCE.toLocalDate();

    /** Cutoff that includes any outbox row scheduled at persist time. */
    public static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2099, 12, 31, 23, 59);

    private static final AtomicInteger PUBLISH_SEQUENCE = new AtomicInteger();

    /**
     * Monotonic publish instants for {@link Given} posts (newest post = highest
     * minute offset).
     */
    public static LocalDateTime nextPublishedAt() {
        return REFERENCE.plusMinutes(PUBLISH_SEQUENCE.getAndIncrement());
    }

    public static void reset() {
        PUBLISH_SEQUENCE.set(0);
    }

    private TestTimes() {}
}
