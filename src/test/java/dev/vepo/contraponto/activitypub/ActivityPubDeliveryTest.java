package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class ActivityPubDeliveryTest {

    @Test
    void markDeliveredClearsLastError() {
        var delivery = new ActivityPubDelivery();
        delivery.markFailed("HTTP 401", LocalDateTime.now());

        delivery.markDelivered();

        assertThat(delivery.getStatus()).isEqualTo(ActivityPubDeliveryStatus.DELIVERED);
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getDeliveredAt()).isNotNull();
    }

    @Test
    void markFailedMarksFailedOnFifthAttemptAndKeepsLastError() {
        var delivery = new ActivityPubDelivery();
        for (var i = 1; i <= 4; i++) {
            delivery.markFailed("HTTP 401", LocalDateTime.now());
        }

        delivery.markFailed("HTTP 401", LocalDateTime.now());

        assertThat(delivery.getAttempts()).isEqualTo(5);
        assertThat(delivery.getStatus()).isEqualTo(ActivityPubDeliveryStatus.FAILED);
        assertThat(delivery.getLastError()).isEqualTo("HTTP 401");
    }

    @Test
    void markFailedReplacesBlankErrorWithFallback() {
        var delivery = new ActivityPubDelivery();

        delivery.markFailed(null, LocalDateTime.now());
        assertThat(delivery.getLastError()).isEqualTo("unknown delivery failure");

        delivery.markFailed("   ", LocalDateTime.now());
        assertThat(delivery.getLastError()).isEqualTo("unknown delivery failure");
    }

    @Test
    void markFailedStoresErrorAndStaysPendingUntilAttemptLimit() {
        var delivery = new ActivityPubDelivery();
        var retryAt = LocalDateTime.of(2026, 7, 8, 12, 0);

        delivery.markFailed("HTTP 401", retryAt);

        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getLastError()).isEqualTo("HTTP 401");
        assertThat(delivery.getStatus()).isEqualTo(ActivityPubDeliveryStatus.PENDING);
        assertThat(delivery.getNextRetryAt()).isEqualTo(retryAt);
    }
}
