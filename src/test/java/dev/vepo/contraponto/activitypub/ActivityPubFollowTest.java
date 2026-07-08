package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class ActivityPubFollowTest {

    @Test
    void reopenAsPendingClearsAcceptAndStoresNewFollowActivityId() {
        var follow = new ActivityPubFollow();
        follow.accept();
        follow.reject();

        follow.reopenAsPending("https://remote.example/follow/again");

        assertThat(follow.getStatus()).isEqualTo(ActivityPubFollowStatus.PENDING);
        assertThat(follow.getAcceptedAt()).isNull();
        assertThat(follow.getFollowActivityId()).isEqualTo("https://remote.example/follow/again");
    }
}
