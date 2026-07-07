package dev.vepo.contraponto.activitypub;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.user.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubActorDocumentBuilder {

    private static final List<String> CONTEXT = List.of("https://www.w3.org/ns/activitystreams",
                                                        "https://w3id.org/security/v1");

    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubActorDocumentBuilder(BlogSubdomainConfig subdomainConfig) {
        this.subdomainConfig = subdomainConfig;
    }

    public Map<String, Object> buildPerson(User user, ActivityPubActor actor) {
        var document = new LinkedHashMap<String, Object>();
        document.put("@context", CONTEXT);
        document.put("id", ActivityPubPaths.actorId(user, subdomainConfig));
        document.put("type", "Person");
        document.put("preferredUsername", user.getUsername());
        document.put("webfinger", ActivityPubPaths.webFingerHandle(user, subdomainConfig));
        document.put("name", user.getName());
        if (user.getProfileDescription() != null && !user.getProfileDescription().isBlank()) {
            document.put("summary", user.getProfileDescription());
        }
        document.put("inbox", ActivityPubPaths.inbox(user, subdomainConfig));
        document.put("outbox", ActivityPubPaths.outbox(user, subdomainConfig));
        document.put("followers", ActivityPubPaths.followers(user, subdomainConfig));
        document.put("following", ActivityPubPaths.following(user, subdomainConfig));
        document.put("url", ActivityPubPaths.profilePageUrl(user, subdomainConfig));
        document.put("discoverable", actor.isFederationEnabled());
        document.put("publicKey", Map.of("id", actor.getPublicKeyId(),
                                         "owner", ActivityPubPaths.actorId(user, subdomainConfig),
                                         "publicKeyPem", actor.getPublicKeyPem()));
        if (user.getMastodonUrl() != null && !user.getMastodonUrl().isBlank()) {
            document.put("sameAs", user.getMastodonUrl());
        }
        return document;
    }
}
