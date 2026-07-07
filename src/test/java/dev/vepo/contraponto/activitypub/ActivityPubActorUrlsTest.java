package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActivityPubActorUrlsTest {

    @Test
    void normalizesTrailingSlash() {
        assertThat(ActivityPubActorUrls.actorUrisMatch("https://remote.example/users/reader/",
                                                       "https://remote.example/users/reader"))
                                                                                              .isTrue();
    }

    @Test
    void stripsKeyIdFragment() {
        assertThat(ActivityPubActorUrls.actorUrlFromKeyId("https://ursal.zone/users/vepo#main-key"))
                                                                                                    .isEqualTo("https://ursal.zone/users/vepo");
    }
}
