package dev.vepo.contraponto.blog;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogSubdomainConfig {

    private static final Set<String> PLATFORM_ONLY_ROOT_SEGMENTS = Set.of("authors", "explore", "robots.txt", "sitemap.xml", "openapi.yaml", "openapi");

    private static final Set<String> SUBDOMAIN_AUTHOR_ROOT_SEGMENTS = Set.of("components", "feed", "main-blog", "page", "post", "serie");

    /**
     * Platform workspace routes served on the author subdomain without /{username}
     * rewrite or platform redirect.
     */
    private static final Set<String> SUBDOMAIN_WORKSPACE_ROOT_SEGMENTS = Set.of("write",
                                                                                "writing",
                                                                                "reading",
                                                                                "manage",
                                                                                "account",
                                                                                "editor",
                                                                                "administration",
                                                                                "search",
                                                                                "library",
                                                                                "dashboard",
                                                                                "profile",
                                                                                "review",
                                                                                "pages",
                                                                                "blogs",
                                                                                "users",
                                                                                "comments",
                                                                                "notifications",
                                                                                "subscriptions",
                                                                                "tags");

    private static final Set<String> SKIP_REWRITE_PREFIXES = Set.of("auth", "js", "style", "images", "i18n", "q", "api", "forms");

    private static final Set<String> SKIP_REWRITE_EXACT = Set.of("favicon.ico", "favicon.svg", "robots.txt");

    private static final String DEFAULT_PUBLIC_ORIGIN_SCHEME = "https";

    private static String hostFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "localhost";
        }
        try {
            return stripPort(URI.create(url.trim()).getHost());
        } catch (IllegalArgumentException _) {
            return "localhost";
        }
    }

    private static String platformPrefixFromHost(String platformHost) {
        var dot = platformHost.indexOf('.');
        if (dot <= 0) {
            return platformHost;
        }
        return platformHost.substring(0, dot);
    }

    private static String stripPort(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        var trimmed = host.trim().toLowerCase();
        var colon = trimmed.indexOf(':');
        if (colon >= 0) {
            return trimmed.substring(0, colon);
        }
        return trimmed;
    }

    private final boolean enabled;
    private final String baseDomain;
    private final String platformHost;

    private final String platformPrefix;

    private final String publicOriginScheme;

    @Inject
    public BlogSubdomainConfig(@ConfigProperty(name = "app.blog-subdomain.enabled", defaultValue = "false") boolean enabled,
                               @ConfigProperty(name = "app.blog-subdomain.base-domain") Optional<String> baseDomain,
                               @ConfigProperty(name = "app.platform.host") Optional<String> configuredPlatformHost,
                               @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String publicSiteUrl) {
        this(enabled, baseDomain.orElse(""), configuredPlatformHost.orElse(""), publicSiteUrl, true);
    }

    BlogSubdomainConfig(boolean enabled,
                        String baseDomain,
                        String configuredPlatformHost,
                        String publicSiteUrl,
                        boolean resolvePlatformHost) {
        this.enabled = enabled;
        this.baseDomain = baseDomain == null ? "" : baseDomain.trim().toLowerCase();
        var resolvedPlatformHost = !resolvePlatformHost || configuredPlatformHost == null || configuredPlatformHost.isBlank()
                                                                                                                              ? hostFromUrl(publicSiteUrl)
                                                                                                                              : stripPort(configuredPlatformHost);
        this.platformHost = resolvedPlatformHost;
        this.platformPrefix = platformPrefixFromHost(resolvedPlatformHost);
        String scheme;
        try {
            scheme = URI.create(publicSiteUrl.trim()).getScheme();
        } catch (IllegalArgumentException _) {
            scheme = DEFAULT_PUBLIC_ORIGIN_SCHEME;
        }
        this.publicOriginScheme = scheme == null || scheme.isBlank() ? DEFAULT_PUBLIC_ORIGIN_SCHEME : scheme;
    }

    public String baseDomain() {
        return baseDomain;
    }

    public boolean enabled() {
        return enabled && !baseDomain.isBlank();
    }

    private boolean isGlobalComponentPath(String normalizedPath) {
        if (!normalizedPath.startsWith("components/")) {
            return false;
        }
        return !normalizedPath.equals("components/grid") && !normalizedPath.startsWith("components/grid/");
    }

    public boolean isPlatformOnlyRootSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return false;
        }
        if (SUBDOMAIN_AUTHOR_ROOT_SEGMENTS.contains(segment)) {
            return false;
        }
        if (SUBDOMAIN_WORKSPACE_ROOT_SEGMENTS.contains(segment)) {
            return false;
        }
        return PLATFORM_ONLY_ROOT_SEGMENTS.contains(segment);
    }

    public String normalizeAuthorSubdomainRequestPath(String authorUsername, String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return path;
        }
        var usernamePrefix = "/%s".formatted(authorUsername);
        if (path.equals(usernamePrefix) || path.equals("%s/".formatted(usernamePrefix))) {
            return "/";
        }
        var nestedPrefix = "%s/".formatted(usernamePrefix);
        if (path.startsWith(nestedPrefix)) {
            return path.substring(usernamePrefix.length());
        }
        return path;
    }

    public Optional<String> parseUserSubdomain(String hostHeader) {
        if (!enabled()) {
            return Optional.empty();
        }
        var host = stripPort(hostHeader);
        if (host.isBlank() || host.equals(platformHost)) {
            return Optional.empty();
        }
        var suffix = ".%s".formatted(baseDomain);
        if (!host.endsWith(suffix)) {
            return Optional.empty();
        }
        var username = host.substring(0, host.length() - suffix.length());
        if (username.isBlank() || username.contains(".") || username.equals(platformPrefix)) {
            return Optional.empty();
        }
        return Optional.of(username);
    }

    public String platformHost() {
        return platformHost;
    }

    public String platformPrefix() {
        return platformPrefix;
    }

    public String platformUrl(String path) {
        var normalized = path == null || path.isBlank() ? "/" : path;
        if (!normalized.startsWith("/")) {
            normalized = "/%s".formatted(normalized);
        }
        return "%s://%s%s".formatted(publicOriginScheme(), platformHost, normalized);
    }

    public String publicOriginScheme() {
        return publicOriginScheme == null || publicOriginScheme.isBlank() ? DEFAULT_PUBLIC_ORIGIN_SCHEME : publicOriginScheme;
    }

    public boolean shouldSkipSubdomainRewrite(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return false;
        }
        var normalized = path.startsWith("/") ? path.substring(1) : path;
        if (SKIP_REWRITE_EXACT.contains(normalized)) {
            return true;
        }
        if (isGlobalComponentPath(normalized)) {
            return true;
        }
        var firstSegment = normalized.indexOf('/') >= 0 ? normalized.substring(0, normalized.indexOf('/')) : normalized;
        if (SUBDOMAIN_WORKSPACE_ROOT_SEGMENTS.contains(firstSegment)) {
            return true;
        }
        for (var prefix : SKIP_REWRITE_PREFIXES) {
            if (normalized.equals(prefix) || normalized.startsWith("%s/".formatted(prefix))) {
                return true;
            }
        }
        return false;
    }

    public String subdomainOrigin(String username) {
        return "%s://%s.%s".formatted(publicOriginScheme(), username, baseDomain);
    }
}
