package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.BlogPaths;
import dev.vepo.contraponto.post.PostPaths;

public enum NotificationType {
    NEW_POST,
    NEW_FOLLOW,
    NEW_SUBSCRIBE,
    NEW_COMMENT,
    COMMON_HIGHLIGHT_PROPOSAL,
    PUBLIC_HIGHLIGHT_NOTE,
    POST_RESPONSE,
    GIT_SYNC_SUCCEEDED,
    GIT_SYNC_FAILED;

    private static String commentLink(Notification notification) {
        if (notification.getPost() != null) {
            return "%s#comments".formatted(PostPaths.extractUrl(notification.getPost()));
        }
        return BlogPaths.extractUrl(notification.getBlog());
    }

    private static String gitSyncLink(Notification notification) {
        if (notification.getGitSyncRun() != null) {
            var run = notification.getGitSyncRun();
            return "/blogs/%s/git-sync/%s".formatted(run.getBlog().getId(), run.getId());
        }
        return BlogPaths.extractUrl(notification.getBlog());
    }

    private static String postLink(Notification notification) {
        if (notification.getPost() != null) {
            return PostPaths.extractUrl(notification.getPost());
        }
        return BlogPaths.extractUrl(notification.getBlog());
    }

    public String linkUrl(Notification notification) {
        return switch (this) {
            case GIT_SYNC_SUCCEEDED, GIT_SYNC_FAILED -> gitSyncLink(notification);
            case NEW_POST -> postLink(notification);
            case NEW_COMMENT -> commentLink(notification);
            case COMMON_HIGHLIGHT_PROPOSAL, PUBLIC_HIGHLIGHT_NOTE, POST_RESPONSE -> "/writing/highlights";
            case NEW_FOLLOW, NEW_SUBSCRIBE -> BlogPaths.extractUrl(notification.getBlog());
        };
    }
}
