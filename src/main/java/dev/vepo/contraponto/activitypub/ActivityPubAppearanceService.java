package dev.vepo.contraponto.activitypub;

import java.util.List;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.user.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubAppearanceService {

    private final ActivityPubSettings settings;
    private final ActivityPubActorRepository actorRepository;
    private final ActivityPubFollowRepository followRepository;
    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubAppearanceService(ActivityPubSettings settings,
                                        ActivityPubActorRepository actorRepository,
                                        ActivityPubFollowRepository followRepository,
                                        BlogSubdomainConfig subdomainConfig) {
        this.settings = settings;
        this.actorRepository = actorRepository;
        this.followRepository = followRepository;
        this.subdomainConfig = subdomainConfig;
    }

    public ActivityPubFederationView buildView(User user) {
        var actorOptional = actorRepository.findByUserId(user.getId());
        var federationEnabled = actorOptional.map(ActivityPubActor::isFederationEnabled).orElse(false);
        var handle = "@%s@%s".formatted(user.getUsername(), federationDomain());
        var actorUrl = ActivityPubPaths.actorId(user, subdomainConfig);
        long followerCount = 0;
        List<ActivityPubFederationView.ActivityPubFollowRequestView> pending = List.of();
        if (actorOptional.isPresent() && federationEnabled) {
            var actor = actorOptional.get();
            followerCount = followRepository.listAcceptedByLocalActor(actor.getId()).size();
            pending = followRepository.listPendingByLocalActor(actor.getId())
                                      .stream()
                                      .map(f -> {
                                          var remote = f.getRemoteActor();
                                          return new ActivityPubFederationView.ActivityPubFollowRequestView(f.getId(),
                                                                                                            remote.getActorId(),
                                                                                                            ActivityPubRemoteHandle.displayLabel(remote),
                                                                                                            ActivityPubRemoteHandle.derivedHandle(remote));
                                      })
                                      .toList();
        }
        return new ActivityPubFederationView(federationEnabled,
                                             settings.enabled(),
                                             handle,
                                             actorUrl,
                                             followerCount,
                                             pending);
    }

    private String federationDomain() {
        if (subdomainConfig.enabled() && !subdomainConfig.baseDomain().isBlank()) {
            return subdomainConfig.baseDomain();
        }
        return subdomainConfig.platformHost();
    }
}
