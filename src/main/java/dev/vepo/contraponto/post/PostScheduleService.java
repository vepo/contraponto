package dev.vepo.contraponto.post;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class PostScheduleService {

    private final Duration minimumLeadTime;

    @Inject
    public PostScheduleService(@ConfigProperty(name = "contraponto.post.schedule.poll-interval", defaultValue = "2m") Duration minimumLeadTime) {
        this.minimumLeadTime = minimumLeadTime;
    }

    @Transactional
    public void cancel(Post post) {
        if (post.getScheduledAt() == null) {
            throw new BadRequestException("Post is not scheduled.");
        }
        post.setScheduledAt(null);
    }

    public LocalDateTime earliestAllowed() {
        return LocalDateTime.now(ZoneOffset.UTC).plus(minimumLeadTime);
    }

    public boolean meetsMinimumLeadTime(LocalDateTime scheduledAt) {
        return scheduledAt != null && !scheduledAt.isBefore(earliestAllowed());
    }

    public Duration minimumLeadTime() {
        return minimumLeadTime;
    }

    @Transactional
    public void schedule(Post post, LocalDateTime scheduledAt) {
        if (post.isPublished()) {
            throw new BadRequestException("Post is already published.");
        }
        post.setScheduledAt(scheduledAt);
    }
}
