package dev.vepo.contraponto.activitypub.discovery;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.ws.rs.core.PathSegment;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.shared.infra.InternalRoutePrefixes;
import dev.vepo.contraponto.user.UsernameValidator;

/**
 * Maps public federation URLs to the internal {@value #INTERNAL_PREFIX} JAX-RS
 * tree. Used by {@link ActivityPubIngressFilter} so ActivityPub and protocol
 * probes never reach {@code BlogEndpoint} or other user-scoped routes.
 */
public final class ActivityPubIngressPaths {

    /**
     * Internal grouping prefix for ActivityPub JAX-RS endpoints (not linked in
     * HTML).
     */
    public static final String INTERNAL_PREFIX = InternalRoutePrefixes.ACTIVITY_PUB;

    private static final Set<String> USER_COLLECTION_SEGMENTS = Set.of("inbox",
                                                                       "outbox",
                                                                       "followers",
                                                                       "following",
                                                                       "activities",
                                                                       "poco");

    private static final String NODEINFO_20 = INTERNAL_PREFIX + "/nodeinfo/2.0";

    private static final String NODEINFO_21 = INTERNAL_PREFIX + "/nodeinfo/2.1";

    private static boolean isUsernameSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return false;
        }
        if (CustomPagePaths.isReservedSegment(segment)) {
            return false;
        }
        return segment.matches("^" + UsernameValidator.USERNAME_PATH_SEGMENT + "$");
    }

    /**
     * Returns whether {@code publicPath} is a federation URL handled by the ingress
     * filter.
     *
     * @param publicPath request path after {@code BlogSubdomainFilter} rewrite (may
     *                   start with {@code /})
     * @return {@code true} when {@link #resolveInternalPath(String)} would return a
     *         value
     */
    public static boolean matches(String publicPath) {
        return resolveInternalPath(publicPath).isPresent();
    }

    private static Optional<String> matchNodeInfo(List<String> segments) {
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        if (segments.size() == 2 && "nodeinfo".equals(segments.get(0))) {
            var version = segments.get(1);
            if ("2.0".equals(version)) {
                return Optional.of(NODEINFO_20);
            }
            if ("2.1".equals(version)) {
                return Optional.of(NODEINFO_21);
            }
        }
        if (segments.size() == 2 && "api".equals(segments.get(0)) && "nodeinfo".equals(segments.get(1))) {
            return Optional.of(NODEINFO_20);
        }
        if (segments.size() == 3
                && isUsernameSegment(segments.get(0))
                && "api".equals(segments.get(1))
                && "nodeinfo".equals(segments.get(2))) {
            return Optional.of(NODEINFO_20);
        }
        return Optional.empty();
    }

    private static Optional<String> matchUserCollection(List<String> segments) {
        if (segments.size() < 2) {
            return Optional.empty();
        }
        var username = segments.get(0);
        if (!isUsernameSegment(username)) {
            return Optional.empty();
        }
        var collection = segments.get(1);
        if (!USER_COLLECTION_SEGMENTS.contains(collection)) {
            return Optional.empty();
        }
        if ("activities".equals(collection)) {
            if (segments.size() != 4) {
                return Optional.empty();
            }
            return Optional.of("%s/user/%s/activities/%s/%s".formatted(INTERNAL_PREFIX,
                                                                       username,
                                                                       segments.get(2),
                                                                       segments.get(3)));
        }
        if (segments.size() != 2) {
            return Optional.empty();
        }
        return Optional.of("%s/user/%s/%s".formatted(INTERNAL_PREFIX, username, collection));
    }

    private static Optional<String> matchWellKnown(List<String> segments) {
        if (segments.size() != 2 || !".well-known".equals(segments.get(0))) {
            return Optional.empty();
        }
        return switch (segments.get(1)) {
            case "webfinger" -> Optional.of("%s/well-known/webfinger".formatted(INTERNAL_PREFIX));
            case "host-meta" -> Optional.of("%s/well-known/host-meta".formatted(INTERNAL_PREFIX));
            case "nodeinfo" -> Optional.of("%s/well-known/nodeinfo".formatted(INTERNAL_PREFIX));
            default -> Optional.empty();
        };
    }

    private static List<String> normalizedSegments(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return List.of();
        }
        var normalized = path.startsWith("/") ? path.substring(1) : path;
        return List.of(normalized.split("/"));
    }

    /**
     * Same as {@link #resolveInternalPath(String)} for JAX-RS {@link PathSegment}
     * lists.
     *
     * @param segments path segments from {@link jakarta.ws.rs.core.UriInfo}
     * @return internal path under {@link #INTERNAL_PREFIX}, or empty when not a
     *         protocol URL
     */
    public static Optional<String> resolveInternalPath(List<PathSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return Optional.empty();
        }
        var parts = segments.stream().map(PathSegment::getPath).toList();
        var path = "/" + String.join("/", parts);
        return resolveInternalPath(path);
    }

    /**
     * If {@code publicPath} is a known federation URL, returns the matching
     * internal JAX-RS path under {@link #INTERNAL_PREFIX}; otherwise empty.
     * <p>
     * This both <em>matches</em> and <em>resolves</em> in one step — callers that
     * only need a yes/no check should use {@link #matches(String)}.
     *
     * @param publicPath request path after {@code BlogSubdomainFilter} rewrite (may
     *                   start with {@code /})
     * @return internal path such as {@code /__activity_pub__/user/alice/inbox}, or
     *         empty when not a protocol URL
     */
    public static Optional<String> resolveInternalPath(String publicPath) {
        var segments = normalizedSegments(publicPath);
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        return matchWellKnown(segments).or(() -> matchNodeInfo(segments)).or(() -> matchUserCollection(segments));
    }

    private ActivityPubIngressPaths() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
