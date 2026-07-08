package dev.vepo.contraponto.activitypub;

import java.util.Optional;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.user.User;

public final class ActivityPubPaths {

    public static final String ACTIVITY_JSON = "application/activity+json";
    public static final String LD_JSON = "application/ld+json";

    public static String acctHandle(User user, BlogSubdomainConfig config) {
        var domain = federationDomain(config);
        return "acct:%s@%s".formatted(user.getUsername(), domain);
    }

    public static String activityId(User user, BlogSubdomainConfig config, String activityType, long sequentialId) {
        return underActor(actorId(user, config), "activities/%s/%d".formatted(activityType, sequentialId));
    }

    public static Optional<String> actorHostAcctHandle(User user, BlogSubdomainConfig config) {
        if (!config.enabled() || config.baseDomain().isBlank()) {
            return Optional.empty();
        }
        var actorHost = "%s.%s".formatted(user.getUsername(), config.baseDomain());
        return Optional.of("acct:%s@%s".formatted(user.getUsername(), actorHost));
    }

    public static String actorId(User user, BlogSubdomainConfig config) {
        if (config.enabled()) {
            return "%s/".formatted(config.subdomainOrigin(user.getUsername()));
        }
        var url = config.platformUrl("/%s".formatted(user.getUsername()));
        return url.endsWith("/") ? url : "%s/".formatted(url);
    }

    private static String federationDomain(BlogSubdomainConfig config) {
        if (config.enabled() && !config.baseDomain().isBlank()) {
            return config.baseDomain();
        }
        return config.platformHost();
    }

    public static String followers(User user, BlogSubdomainConfig config) {
        return "%sfollowers".formatted(actorId(user, config));
    }

    public static String following(User user, BlogSubdomainConfig config) {
        return "%sfollowing".formatted(actorId(user, config));
    }

    public static String inbox(User user, BlogSubdomainConfig config) {
        return "%sinbox".formatted(actorId(user, config));
    }

    public static boolean matchesFederationAcct(User user, BlogSubdomainConfig config, String resource) {
        if (resource == null || resource.isBlank()) {
            return false;
        }
        if (resource.equalsIgnoreCase(acctHandle(user, config))) {
            return true;
        }
        return actorHostAcctHandle(user, config).map(alias -> resource.equalsIgnoreCase(alias)).orElse(false);
    }

    public static String outbox(User user, BlogSubdomainConfig config) {
        return "%soutbox".formatted(actorId(user, config));
    }

    public static String outboxPage(User user, BlogSubdomainConfig config, int page) {
        if (page <= 1) {
            return outbox(user, config);
        }
        return "%soutbox?page=%d".formatted(actorId(user, config), page);
    }

    public static String postObjectId(Post post, BlogSubdomainConfig config) {
        return config.platformUrl(PostPaths.extractUrl(post));
    }

    public static String profilePageUrl(User user, BlogSubdomainConfig config) {
        return config.platformUrl("/authors/%s".formatted(user.getUsername()));
    }

    public static String publicKeyId(User user, BlogSubdomainConfig config) {
        return "%s#mainKey".formatted(actorId(user, config));
    }

    public static String remoteFollowSubscribeTemplate(User user, BlogSubdomainConfig config) {
        return "%s?acct={uri}".formatted(profilePageUrl(user, config));
    }

    private static String underActor(String actorBase, String relativePath) {
        var base = actorBase.endsWith("/") ? actorBase : "%s/".formatted(actorBase);
        var rel = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return base + rel;
    }

    public static String webFingerHandle(User user, BlogSubdomainConfig config) {
        return "%s@%s".formatted(user.getUsername(), federationDomain(config));
    }

    public static String webFingerResource(User user, BlogSubdomainConfig config) {
        return acctHandle(user, config);
    }

    private ActivityPubPaths() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
