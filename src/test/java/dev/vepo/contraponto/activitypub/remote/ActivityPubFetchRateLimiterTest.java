package dev.vepo.contraponto.activitypub.remote;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActivityPubFetchRateLimiterTest {

    @Test
    void blocksExcessFetchesPerDomain() {
        var settings = new ActivityPubFetchSettings(java.time.Duration.ofSeconds(1),
                                                    java.time.Duration.ofSeconds(1),
                                                    2,
                                                    7);
        var limiter = new ActivityPubFetchRateLimiter(settings);
        assertThat(limiter.tryAcquire("example.com")).isTrue();
        assertThat(limiter.tryAcquire("example.com")).isTrue();
        assertThat(limiter.tryAcquire("example.com")).isFalse();
    }
}
