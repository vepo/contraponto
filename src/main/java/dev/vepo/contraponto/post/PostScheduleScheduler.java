package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.git.GitSyncTrigger;
import dev.vepo.contraponto.git.PostGitSyncRequestedEvent;
import dev.vepo.contraponto.notification.NotificationService;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class PostScheduleScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PostScheduleScheduler.class);

    private final PostRepository postRepository;
    private final PostPublicationService publicationService;
    private final NotificationService notificationService;
    private final Event<PostGitSyncRequestedEvent> postGitSyncEvents;

    @Inject
    public PostScheduleScheduler(PostRepository postRepository,
                                 PostPublicationService publicationService,
                                 NotificationService notificationService,
                                 Event<PostGitSyncRequestedEvent> postGitSyncEvents) {
        this.postRepository = postRepository;
        this.publicationService = publicationService;
        this.notificationService = notificationService;
        this.postGitSyncEvents = postGitSyncEvents;
    }

    @Scheduled(every = "${contraponto.post.schedule.poll-interval}", concurrentExecution = ConcurrentExecution.SKIP)
    void publishDueScheduledPosts() {
        for (Post post : postRepository.findDueScheduled(LocalDateTime.now(ZoneOffset.UTC))) {
            publishOne(post);
        }
    }

    private void publishOne(Post post) {
        try {
            publicationService.publish(post);
            postGitSyncEvents.fire(new PostGitSyncRequestedEvent(post.getId(), GitSyncTrigger.PUBLISH));
            notificationService.notifySchedulePublishSucceeded(post.getAuthor(), post);
        } catch (RuntimeException ex) {
            logger.warn("Scheduled publish failed postId={}", post.getId(), ex);
            notificationService.notifySchedulePublishFailed(post.getAuthor(), post);
        }
    }
}
