package dev.vepo.contraponto.activitypub.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class ActivityPubWebFingerServiceTest {

    @Test
    void canonicalHttpsResourceNormalizesTrailingSlash() {
        assertThat(ActivityPubWebFingerService.canonicalHttpsResource("https://vepo.example/"))
                                                                                               .isEqualTo("https://vepo.example/");
        assertThat(ActivityPubWebFingerService.canonicalHttpsResource("https://blogs.example/authors/vepo/"))
                                                                                                             .isEqualTo("https://blogs.example/authors/vepo");
    }
}
