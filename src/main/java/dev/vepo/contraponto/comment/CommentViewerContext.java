package dev.vepo.contraponto.comment;

public record CommentViewerContext(Long userId, boolean postOwner) {

    public static CommentViewerContext anonymous() {
        return new CommentViewerContext(null, false);
    }

    public static CommentViewerContext of(Long userId, boolean postOwner) {
        return new CommentViewerContext(userId, postOwner);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }
}
