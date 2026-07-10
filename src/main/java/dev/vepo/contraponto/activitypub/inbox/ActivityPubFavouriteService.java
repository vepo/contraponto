package dev.vepo.contraponto.activitypub.inbox;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import dev.vepo.contraponto.activitypub.ActivityPubSettings;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActor;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorRepository;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteActor;
import dev.vepo.contraponto.activitypub.remote.ActivityPubRemoteHandle;
import dev.vepo.contraponto.post.Post;

@ApplicationScoped
public class ActivityPubFavouriteService {

    private final ActivityPubFavouriteRepository favouriteRepository;
    private final ActivityPubPostResolver postResolver;
    private final ActivityPubSettings settings;
    private final ActivityPubActorRepository actorRepository;

    @Inject
    public ActivityPubFavouriteService(ActivityPubFavouriteRepository favouriteRepository,
                                       ActivityPubPostResolver postResolver,
                                       ActivityPubSettings settings,
                                       ActivityPubActorRepository actorRepository) {
        this.favouriteRepository = favouriteRepository;
        this.postResolver = postResolver;
        this.settings = settings;
        this.actorRepository = actorRepository;
    }

    public FediverseFavouritePostView buildPostView(Post post) {
        if (!settings.enabled()) {
            return FediverseFavouritePostView.hidden();
        }
        var authorUserId = post.getBlog().getOwner().getId();
        var actor = actorRepository.findByUserId(authorUserId);
        if (actor.isEmpty() || !actor.get().isFederationEnabled()) {
            return FediverseFavouritePostView.hidden();
        }
        return new FediverseFavouritePostView(true, favouriteRepository.countByPostId(post.getId()));
    }

    public long countByPostId(long postId) {
        return favouriteRepository.countByPostId(postId);
    }

    /**
     * Author-only remote handles who favourited the post (Fediverse favourite
     * list).
     */
    public List<String> listRemoteHandlesForAuthor(Post post, Long viewerUserId) {
        if (!settings.enabled()) {
            throw new NotFoundException("Fediverse favourites not available");
        }
        var authorUserId = post.getBlog().getOwner().getId();
        var actor = actorRepository.findByUserId(authorUserId);
        if (actor.isEmpty() || !actor.get().isFederationEnabled()) {
            throw new NotFoundException("Fediverse favourites not available");
        }
        if (viewerUserId == null || !viewerUserId.equals(authorUserId)) {
            throw new ForbiddenException("Only the post author can see who favourited on the Fediverse.");
        }
        return favouriteRepository.listByPostIdWithRemoteActor(post.getId())
                                  .stream()
                                  .map(f -> ActivityPubRemoteHandle.derivedHandle(f.getRemoteActor()))
                                  .toList();
    }

    @Transactional
    public void recordLike(ActivityPubActor localActor,
                           ActivityPubRemoteActor remoteActor,
                           String objectUri,
                           String likeActivityId) {
        if (likeActivityId == null || likeActivityId.isBlank()) {
            return;
        }
        var owner = localActor.getUser();
        postResolver.resolvePublishedPostOwnedBy(objectUri, owner)
                    .ifPresent(post -> favouriteRepository.saveLike(post, remoteActor, likeActivityId));
    }

    @Transactional
    public void removeLike(ActivityPubActor localActor, ActivityPubRemoteActor remoteActor, String objectUri) {
        var owner = localActor.getUser();
        postResolver.resolvePublishedPostOwnedBy(objectUri, owner)
                    .flatMap(post -> favouriteRepository.findByPostAndRemote(post.getId(), remoteActor.getId()))
                    .ifPresent(favouriteRepository::delete);
    }

    public boolean wouldRemoveFavourite(ActivityPubActor localActor,
                                        ActivityPubRemoteActor remoteActor,
                                        String objectUri) {
        var owner = localActor.getUser();
        return postResolver.resolvePublishedPostOwnedBy(objectUri, owner)
                           .flatMap(post -> favouriteRepository.findByPostAndRemote(post.getId(), remoteActor.getId()))
                           .isPresent();
    }
}
