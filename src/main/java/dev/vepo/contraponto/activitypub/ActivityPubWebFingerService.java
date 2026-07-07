package dev.vepo.contraponto.activitypub;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.user.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubWebFingerService {

    private record AcctResource(String username, String domain) {}

    static final String PROFILE_PAGE_REL = "http://webfinger.net/rel/profile-page";

    private final ActivityPubSettings settings;
    private final ActivityPubActorService actorService;
    private final UserRepository userRepository;

    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubWebFingerService(ActivityPubSettings settings,
                                       ActivityPubActorService actorService,
                                       UserRepository userRepository,
                                       BlogSubdomainConfig subdomainConfig) {
        this.settings = settings;
        this.actorService = actorService;
        this.userRepository = userRepository;
        this.subdomainConfig = subdomainConfig;
    }

    public Map<String, Object> hostMetaLinks() {
        var template = subdomainConfig.enabled() && !subdomainConfig.baseDomain().isBlank()
                                                                                            ? "https://%s/.well-known/webfinger?resource={uri}".formatted(subdomainConfig.baseDomain())
                                                                                            : "%s/.well-known/webfinger?resource={uri}".formatted(subdomainConfig.platformUrl("/"));
        return Map.of("links", List.of(Map.of("rel", "lrdd",
                                              "template", template)));
    }

    private Optional<AcctResource> parseAcctResource(String resource) {
        if (!resource.toLowerCase().startsWith("acct:")) {
            return Optional.empty();
        }
        var body = resource.substring("acct:".length());
        var at = body.lastIndexOf('@');
        if (at <= 0 || at >= body.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(new AcctResource(body.substring(0, at), body.substring(at + 1)));
    }

    public Optional<Map<String, Object>> resolve(String resource) {
        if (!settings.enabled() || resource == null || resource.isBlank()) {
            return Optional.empty();
        }
        var acct = parseAcctResource(resource);
        if (acct.isEmpty()) {
            return Optional.empty();
        }
        var username = acct.get().username();
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        var actor = actorService.findEnabledByUsername(username);
        if (actor.isEmpty()) {
            return Optional.empty();
        }
        if (!ActivityPubPaths.matchesFederationAcct(user, subdomainConfig, resource)) {
            return Optional.empty();
        }
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var profilePage = ActivityPubPaths.profilePageUrl(user, subdomainConfig);
        var response = new LinkedHashMap<String, Object>();
        response.put("subject", resource);
        response.put("links", List.of(Map.of("rel", "self",
                                             "type", ActivityPubPaths.ACTIVITY_JSON,
                                             "href", actorId),
                                      Map.of("rel", PROFILE_PAGE_REL,
                                             "type", "text/html",
                                             "href", profilePage)));
        return Optional.of(response);
    }
}
