package dev.vepo.contraponto.post;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostEngagementService {

    /**
     * Reader engagement metrics exclude the post author viewing their own work.
     */
    public boolean shouldRecordReaderEngagement(Post post, Long viewerUserId) {
        if (viewerUserId == null) {
            return true;
        }
        return !post.getAuthor().getId().equals(viewerUserId);
    }
}
