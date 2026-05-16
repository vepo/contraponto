package dev.vepo.contraponto.comment;

import java.time.LocalDateTime;

public record CommentView(long id,
                          long postId,
                          String authorName,
                          String authorUsername,
                          String body,
                          LocalDateTime createdAt,
                          CommentStatus status,
                          long replyCount,
                          boolean canModerate,
                          boolean pendingOwn,
                          int depth,
                          Long parentId) {}
