package dev.vepo.contraponto.activitypub.remote;

import java.net.URI;

public final class ActivityPubRemoteHandle {

    public static String derivedHandle(ActivityPubRemoteActor remote) {
        var preferred = remote.getPreferredUsername();
        var actorId = remote.getActorId();
        if (preferred != null && !preferred.isBlank() && actorId != null) {
            try {
                var host = URI.create(actorId).getHost();
                if (host != null && !host.isBlank()) {
                    return "@%s@%s".formatted(preferred, host);
                }
            } catch (IllegalArgumentException ignored) {
                // fall through to actor id
            }
        }
        return actorId;
    }

    public static String displayLabel(ActivityPubRemoteActor remote) {
        var name = remote.getDisplayName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        var preferred = remote.getPreferredUsername();
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return remote.getActorId();
    }

    private ActivityPubRemoteHandle() {}
}
