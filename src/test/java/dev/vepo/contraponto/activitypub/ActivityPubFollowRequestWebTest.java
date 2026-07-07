package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@WebAuthTest
class ActivityPubFollowRequestWebTest {

    private static final String TEST_USER_PASSWORD = "fediversePass123";

    @Inject
    ActivityPubActorRepository actorRepository;

    @Inject
    ActivityPubFollowRepository followRepository;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    private User testUser;

    @Test
    void appearancePanelShowsRemoteDisplayNameAndHandle(App app) {
        app.login(testUser)
           .writingAppearance()
           .assertFediverseFollowRequestVisible("Remote Reader", "@reader@mastodon.social");
    }

    @BeforeEach
    @Transactional
    void setUp() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername("followpaneluser")
                        .withEmail("followpanel@example.com")
                        .withPassword(TEST_USER_PASSWORD)
                        .withName("Follow Panel User")
                        .persist();
        var actor = Given.activityPubActor().withUser(testUser).persist();
        var remote = new ActivityPubRemoteActor("https://mastodon.social/users/reader",
                                                "https://mastodon.social/inbox");
        remote.applyFetchedProfile("https://mastodon.social/inbox",
                                   "-----BEGIN PUBLIC KEY-----\nTEST\n-----END PUBLIC KEY-----",
                                   "https://mastodon.social/users/reader#main-key",
                                   "Remote Reader",
                                   "reader");
        remoteActorRepository.create(remote);
        followRepository.create(new ActivityPubFollow(actor,
                                                      remote,
                                                      ActivityPubFollowStatus.PENDING,
                                                      "https://mastodon.social/follow/pending"));
        assertThat(followRepository.listPendingByLocalActor(actor.getId())).hasSize(1);
    }
}
