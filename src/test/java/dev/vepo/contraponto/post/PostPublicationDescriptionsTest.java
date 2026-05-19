package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostPublicationDescriptionsTest {

    @Test
    void exceedsPublicationLimitDetectsLongDescriptions() {
        assertThat(PostPublicationDescriptions.exceedsPublicationLimit(null)).isFalse();
        assertThat(PostPublicationDescriptions.exceedsPublicationLimit("short")).isFalse();
        assertThat(PostPublicationDescriptions.exceedsPublicationLimit("x".repeat(513))).isTrue();
    }

    @Test
    void truncateForPublicationHandlesNullEmptyAndWithinLimit() {
        assertThat(PostPublicationDescriptions.truncateForPublication(null)).isEmpty();
        assertThat(PostPublicationDescriptions.truncateForPublication("")).isEmpty();
        String within = "x".repeat(512);
        assertThat(PostPublicationDescriptions.truncateForPublication(within)).isEqualTo(within);
    }

    @Test
    void truncateForPublicationHardTruncatesBeyondLimit() {
        String longDesc = "a".repeat(551);
        String truncated = PostPublicationDescriptions.truncateForPublication(longDesc);
        assertThat(truncated).hasSize(512);
        assertThat(truncated).isEqualTo("a".repeat(512));
    }

    @Test
    void truncateForPublicationStripsTrailingWhitespaceBeforeMeasuring() {
        String padded = "hello" + " ".repeat(600);
        assertThat(PostPublicationDescriptions.truncateForPublication(padded)).isEqualTo("hello");
        assertThat(PostPublicationDescriptions.exceedsPublicationLimit(padded)).isFalse();
    }
}
