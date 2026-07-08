package dev.vepo.contraponto.activitypub.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubActivityType;
import dev.vepo.contraponto.activitypub.ActivityPubFollowStatus;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActor;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorRepository;
import dev.vepo.contraponto.activitypub.inbox.ActivityPubFollow;
import dev.vepo.contraponto.activitypub.inbox.ActivityPubFollowRepository;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActor;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActorRepository;
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

    @Inject
    ActivityPubDeliveryService deliveryService;

    private User user;
    private ActivityPubActor actor;
    private Post post;

    @Test
    void enqueueHistoricalPostsForAcceptedFollow() {
        Given.transaction(() -> {
            var managedActor = actorRepository.findByUserId(user.getId()).orElseThrow();
            var remote = remoteActorRepository.create(new ActivityPubRemoteActor("https://remote.example/users/backfill",
                                                                                 "https://remote.example/inbox-backfill"));
            var follow = new ActivityPubFollow(managedActor, remote, ActivityPubFollowStatus.ACCEPTED, "https://remote.example/follow/backfill");
            follow.accept();
            followRepository.create(follow);

            deliveryService.enqueueHistoricalPostsForAcceptedFollow(follow);

            assertThat(deliveryRepository.countPendingForActor(actor.getId())).isEqualTo(1);
            var pending = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusMinutes(1));
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getActivityType()).isEqualTo(ActivityPubActivityType.CREATE);
            assertThat(pending.get(0).getTargetInboxUrl()).isEqualTo("https://remote.example/inbox-backfill");
            assertThat(pending.get(0).getPayloadJson()).contains("Fediverse Post");
        });
    }

    @Test
    void enqueueHistoricalPostsIncludesSecondaryBlogArchive() {
        var secondary = Given.blog()
                             .withUser(user)
                             .withSlug("lab-notes")
                             .withName("Lab Notes")
                             .withDescription("Secondary blog archive")
                             .persist();
        Given.post()
             .withAuthor(user)
             .withBlog(secondary)
             .withTitle("Secondary Archive Note")
             .withSlug("secondary-archive-note")
             .withDescription("Ignored in Create content")
             .withContent("Body")
             .persist();

        Given.transaction(() -> {
            var managedActor = actorRepository.findByUserId(user.getId()).orElseThrow();
            var remote = remoteActorRepository.create(new ActivityPubRemoteActor("https://remote.example/users/multi-blog",
                                                                                 "https://remote.example/inbox-multi-blog"));
            var follow = new ActivityPubFollow(managedActor,
                                               remote,
                                               ActivityPubFollowStatus.ACCEPTED,
                                               "https://remote.example/follow/multi-blog");
            follow.accept();
            followRepository.create(follow);

            deliveryService.enqueueHistoricalPostsForAcceptedFollow(follow);

            var pending = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusMinutes(1));
            var creates = pending.stream()
                                 .filter(delivery -> delivery.getActivityType() == ActivityPubActivityType.CREATE)
                                 .toList();
            assertThat(creates).hasSize(2);
            assertThat(creates.get(0).getPayloadJson()).contains("Fediverse Post");
            assertThat(creates.get(1).getPayloadJson()).contains("Secondary Archive Note")
                                                       .contains("Lab Notes")
                                                       .contains("/deluser/lab-notes/post/secondary-archive-note")
                                                       .contains("\"published\"");
        });
    }

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
    void enqueueOnPublishEventForSecondaryBlog() {
        var secondary = Given.blog()
                             .withUser(user)
                             .withSlug("notes")
                             .withName("Notes")
                             .withDescription("Secondary live fan-out")
                             .persist();
        var secondaryPost = Given.post()
                                 .withAuthor(user)
                                 .withBlog(secondary)
                                 .withTitle("Live Secondary Create")
                                 .withSlug("live-secondary-create")
                                 .withDescription("Summary")
                                 .withContent("Body")
                                 .persist();

        Given.transaction(() -> {
            deliveryObserver.afterPublish(new PostPublishedEvent(secondaryPost.getId(),
                                                                 secondaryPost.getLivePublication().getId(),
                                                                 secondary.getId(),
                                                                 user.getId()));
            assertThat(deliveryRepository.countPendingForActor(actor.getId())).isEqualTo(1);
            var pending = deliveryRepository.findPendingReady(java.time.LocalDateTime.now().plusMinutes(1));
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getActivityType()).isEqualTo(ActivityPubActivityType.CREATE);
            assertThat(pending.get(0).getPayloadJson()).contains("Live Secondary Create")
                                                       .contains("Notes")
                                                       .contains("/deluser/notes/post/live-secondary-create");
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
