package dev.vepo.contraponto.activitypub.discovery;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.activitypub.ActivityPubSettings;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorService;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;

@ApplicationScoped
public class ActivityPubWebFingerService {

    private record AcctResource(String username, String domain) {}

    static final String PROFILE_PAGE_REL = "http://webfinger.net/rel/profile-page";

    static final String SUBSCRIBE_REL = "http://ostatus.org/schema/1.0/subscribe";

    static String canonicalHttpsResource(String resource) {
        var uri = URI.create(resource.trim());
        var scheme = uri.getScheme();
        if (scheme == null || scheme.isBlank()) {
            throw new IllegalArgumentException("Missing scheme");
        }
        var host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Missing host");
        }
        var port = uri.getPort();
        var authority = port > 0 && port != 443 && port != 80 ? "%s:%d".formatted(host.toLowerCase(), port) : host.toLowerCase();
        var path = uri.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        } else if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return "%s://%s%s".formatted(scheme.toLowerCase(), authority, path);
    }

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

    private Optional<WebFingerJrd> buildJrdForUser(User user, String subject) {
        var actor = actorService.findEnabledByUserId(user.getId());
        if (actor.isEmpty()) {
            return Optional.empty();
        }
        var actorId = ActivityPubPaths.actorId(user, subdomainConfig);
        var profilePage = ActivityPubPaths.profilePageUrl(user, subdomainConfig);
        return Optional.of(new WebFingerJrd(subject,
                                            List.of(actorId, profilePage),
                                            List.of(WebFingerLink.hrefLink("self",
                                                                           ActivityPubPaths.ACTIVITY_JSON,
                                                                           actorId),
                                                    WebFingerLink.hrefLink(PROFILE_PAGE_REL,
                                                                           "text/html",
                                                                           profilePage),
                                                    WebFingerLink.templateLink(SUBSCRIBE_REL,
                                                                               ActivityPubPaths.remoteFollowSubscribeTemplate(user,
                                                                                                                              subdomainConfig)))));
    }

    public Map<String, Object> hostMetaLinks() {
        var template = subdomainConfig.enabled() && !subdomainConfig.baseDomain().isBlank()
                                                                                            ? "https://%s/.well-known/webfinger?resource={uri}".formatted(subdomainConfig.baseDomain())
                                                                                            : "%s/.well-known/webfinger?resource={uri}".formatted(subdomainConfig.platformUrl("/"));
        return Map.of("links", List.of(Map.of("rel", "lrdd",
                                              "template", template)));
    }

    private boolean matchesHttpsProfile(User user, String resource) {
        try {
            var canonical = canonicalHttpsResource(resource);
            var profile = canonicalHttpsResource(ActivityPubPaths.profilePageUrl(user, subdomainConfig));
            var actor = canonicalHttpsResource(ActivityPubPaths.actorId(user, subdomainConfig));
            var subdomainRoot = canonicalHttpsResource("%s/".formatted(subdomainConfig.subdomainOrigin(user.getUsername())));
            return canonical.equals(profile) || canonical.equals(actor) || canonical.equals(subdomainRoot);
        } catch (IllegalArgumentException _) {
            return false;
        }
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

    public Optional<WebFingerJrd> resolve(String resource) {
        if (!settings.enabled() || resource == null || resource.isBlank()) {
            return Optional.empty();
        }
        var acct = parseAcctResource(resource);
        if (acct.isPresent()) {
            return resolveAcct(acct.get(), resource);
        }
        return resolveHttpsProfile(resource);
    }

    private Optional<WebFingerJrd> resolveAcct(AcctResource acct, String subject) {
        var username = acct.username();
        var user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        if (!ActivityPubPaths.matchesFederationAcct(user, subdomainConfig, subject)) {
            return Optional.empty();
        }
        return buildJrdForUser(user, subject);
    }

    private Optional<String> resolveAuthorSubdomainUsername(URI uri) {
        var host = uri.getHost();
        if (host == null) {
            return Optional.empty();
        }
        var subdomainUser = subdomainConfig.parseUserSubdomain(host);
        if (subdomainUser.isEmpty()) {
            return Optional.empty();
        }
        var path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return subdomainUser;
        }
        return Optional.empty();
    }

    private Optional<WebFingerJrd> resolveHttpsProfile(String resource) {
        URI uri;
        try {
            uri = URI.create(resource.trim());
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
        var scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
            return Optional.empty();
        }
        var username = resolveAuthorSubdomainUsername(uri).or(() -> resolvePlatformAuthorUsername(uri));
        if (username.isEmpty()) {
            return Optional.empty();
        }
        var user = userRepository.findByUsernameIgnoreCase(username.get()).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        if (!matchesHttpsProfile(user, resource)) {
            return Optional.empty();
        }
        return buildJrdForUser(user, resource.trim());
    }

    private Optional<String> resolvePlatformAuthorUsername(URI uri) {
        var host = uri.getHost();
        if (host == null || !host.equalsIgnoreCase(subdomainConfig.platformHost())) {
            return Optional.empty();
        }
        var path = uri.getPath();
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        var normalized = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        var prefix = "/authors/";
        if (!normalized.startsWith(prefix)) {
            return Optional.empty();
        }
        var username = normalized.substring(prefix.length());
        if (username.isBlank() || username.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(username);
    }
}
