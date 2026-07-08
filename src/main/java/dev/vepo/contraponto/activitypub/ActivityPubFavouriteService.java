package dev.vepo.contraponto.activitypub;

import java.util.List;

import dev.vepo.contraponto.post.Post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    public FediverseFavouritePostView buildPostView(Post post, Long viewerUserId) {
        if (!settings.enabled()) {
            return FediverseFavouritePostView.hidden();
        }
        var authorUserId = post.getBlog().getOwner().getId();
        var actor = actorRepository.findByUserId(authorUserId);
        if (actor.isEmpty() || !actor.get().isFederationEnabled()) {
            return FediverseFavouritePostView.hidden();
        }
        var count = favouriteRepository.countByPostId(post.getId());
        var handles = List.<String>of();
        if (viewerUserId != null && viewerUserId.equals(authorUserId)) {
            handles = favouriteRepository.listByPostIdWithRemoteActor(post.getId())
                                         .stream()
                                         .map(f -> ActivityPubRemoteHandle.derivedHandle(f.getRemoteActor()))
                                         .toList();
        }
        return new FediverseFavouritePostView(true, count, handles);
    }

    public long countByPostId(long postId) {
        return favouriteRepository.countByPostId(postId);
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
