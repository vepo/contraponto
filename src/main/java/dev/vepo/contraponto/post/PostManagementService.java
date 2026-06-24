package dev.vepo.contraponto.post;

import dev.vepo.contraponto.git.BlogGitIntegrationTransaction;
import dev.vepo.contraponto.git.GitSyncTrigger;
import dev.vepo.contraponto.git.PostGitSyncRequestedEvent;
import dev.vepo.contraponto.notification.PostUnpublishedEvent;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PostManagementService {

    private final PostRepository postRepository;
    private final PostAccess postAccess;
    private final BlogGitIntegrationTransaction gitIntegrationTransaction;
    private final Event<PostUnpublishedEvent> postUnpublishedEvents;
    private final Event<PostGitSyncRequestedEvent> postGitSyncEvents;

    @Inject
    public PostManagementService(PostRepository postRepository,
                                 PostAccess postAccess,
                                 BlogGitIntegrationTransaction gitIntegrationTransaction,
                                 Event<PostUnpublishedEvent> postUnpublishedEvents,
                                 Event<PostGitSyncRequestedEvent> postGitSyncEvents) {
        this.postRepository = postRepository;
        this.postAccess = postAccess;
        this.gitIntegrationTransaction = gitIntegrationTransaction;
        this.postUnpublishedEvents = postUnpublishedEvents;
        this.postGitSyncEvents = postGitSyncEvents;
    }

    @Transactional
    public void delete(long postId, LoggedUser user) {
        var post = requireOwnedPost(postId, user);
        if (post.isPublished()) {
            throw new BadRequestException("Published posts cannot be deleted. Unpublish first.");
        }
        gitIntegrationTransaction.runScheduledExport(post.getId(), GitSyncTrigger.DRAFT_SAVE);
        postRepository.delete(post.getId());
    }

    public Post requireOwnedPost(long postId, LoggedUser user) {
        var post = postRepository.findByIdWithBlog(postId)
                                 .orElseThrow(() -> new NotFoundException("Post not found! id=%s".formatted(postId)));
        if (!postAccess.canManage(post, user)) {
            throw new NotFoundException("Post not found! id=%s".formatted(postId));
        }
        return post;
    }

    @Transactional
    public void unpublish(long postId, LoggedUser user) {
        var post = requireOwnedPost(postId, user);
        if (!post.isPublished()) {
            throw new BadRequestException("Post is already a draft.");
        }
        post.setPublished(false);
        post.setFeatured(false);
        postUnpublishedEvents.fire(new PostUnpublishedEvent(post.getId(),
                                                            post.getBlog().getId(),
                                                            post.getBlog().getOwner().getId()));
        postGitSyncEvents.fire(new PostGitSyncRequestedEvent(post.getId(), GitSyncTrigger.DRAFT_SAVE));
    }
}
