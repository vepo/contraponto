package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.notification.PostUnpublishedEvent;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubDeliveryServiceTest {

    @Inject
    ActivityPubDeliveryObserver deliveryObserver;

    @Inject
    ActivityPubDeliveryRepository deliveryRepository;

    @Inject
    ActivityPubFollowRepository followRepository;

    @Inject
    ActivityPubRemoteActorRepository remoteActorRepository;

    @Inject
    ActivityPubActorRepository actorRepository;

    private User user;
    private ActivityPubActor actor;
    private Post post;

    @Test
    void enqueueOnPublishEvent() {
        Given.transaction(() -> {
            deliveryObserver.afterPublish(new PostPublishedEvent(post.getId(),
                                                                 post.getLivePublication().getId(),
                                                                 post.getBlog().getId(),
                                                                 user.getId()));
            assertThat(deliveryRepository.countPendingForActor(actor.getId())).isEqualTo(1);
        });
    }

    @Test
    void enqueueOnUnpublishEvent() {
        Given.transaction(() -> {
            deliveryObserver.afterUnpublish(new PostUnpublishedEvent(post.getId(), post.getBlog().getId(), user.getId()));
            assertThat(deliveryRepository.countPendingForActor(actor.getId())).isEqualTo(1);
            var pending = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusMinutes(1));
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getActivityType()).isEqualTo(ActivityPubActivityType.DELETE);
            assertThat(pending.get(0).getPayloadJson()).contains("\"type\":\"Delete\"");
        });
    }

    private void seedAcceptedFollower() {
        Given.transaction(() -> {
            var managedActor = actorRepository.findByUserId(user.getId()).orElseThrow();
            var remote = remoteActorRepository.create(new ActivityPubRemoteActor("https://remote.example/users/follower",
                                                                                 "https://remote.example/inbox"));
            var follow = new ActivityPubFollow(managedActor, remote, ActivityPubFollowStatus.ACCEPTED, "https://remote.example/follow/1");
            follow.accept();
            followRepository.create(follow);
        });
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        user = Given.user()
                    .withUsername("deluser")
                    .withEmail("deluser@example.com")
                    .withPassword("pw123456789")
                    .withName("Delivery User")
                    .persist();
        actor = Given.activityPubActor().withUser(user).persist();
        post = Given.post()
                    .withAuthor(user)
                    .withTitle("Fediverse Post")
                    .withSlug("fed-post")
                    .withDescription("Summary")
                    .withContent("Body")
                    .persist();
        seedAcceptedFollower();
    }
}
