package dev.vepo.contraponto.comment;

import java.time.LocalDateTime;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;

public record CommentManageRow(long commentId,
                               long postId,
                               String postTitle,
                               String postUrl,
                               String authorName,
                               String authorUsername,
                               String bodyPreview,
                               LocalDateTime createdAt,
                               boolean reply) {

    private static final int BODY_PREVIEW_MAX = 200;

    public static CommentManageRow from(PostComment comment) {
        Post post = comment.getPost();
        String body = comment.getBody();
        String preview = body.length() <= BODY_PREVIEW_MAX ? body : "%s…".formatted(body.substring(0, BODY_PREVIEW_MAX - 1));
        return new CommentManageRow(comment.getId(),
                                    post.getId(),
                                    post.getTitle(),
                                    PostEndpoint.extractUrl(post),
                                    comment.getAuthor().getName(),
                                    comment.getAuthor().getUsername(),
                                    preview,
                                    comment.getCreatedAt(),
                                    !comment.isRoot());
    }
}
