package dev.vepo.contraponto.readinglist;

import java.time.LocalDateTime;

public record ReadingListRow(long itemId,
                             long postId,
                             String postTitle,
                             String postSlug,
                             String blogSlug,
                             boolean mainBlog,
                             String authorUsername,
                             String authorName,
                             String blogName,
                             LocalDateTime savedAt,
                             LocalDateTime readAt,
                             boolean postPublished,
                             boolean blogActive) {

    public boolean available() {
        return postPublished && blogActive;
    }

    public boolean unread() {
        return readAt == null;
    }

    public String postUrl() {
        if (mainBlog) {
            return "/%s/post/%s".formatted(authorUsername, postSlug);
        }
        return "/%s/%s/post/%s".formatted(authorUsername, blogSlug, postSlug);
    }
}
