package dev.vepo.contraponto.activitypub.actor;

import java.util.List;

public record ActivityPubFederationView(boolean federationEnabled,
                                        boolean platformFederationEnabled,
                                        String handle,
                                        String actorUrl,
                                        long followerCount,
                                        List<ActivityPubFollowRequestView> pendingFollowRequests) {

    public record ActivityPubFollowRequestView(long followId,
                                               String remoteActorId,
                                               String displayName,
                                               String remoteHandle) {}
}
