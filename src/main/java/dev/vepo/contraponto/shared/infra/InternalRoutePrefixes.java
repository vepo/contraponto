package dev.vepo.contraponto.shared.infra;

/**
 * Canonical internal JAX-RS path prefixes for {@code @PreMatching} ingress
 * filters.
 * <p>
 * Pattern: {@code /__{feature_snake_case}__} — double underscores, feature name
 * in snake_case, never linked in HTML or SEO. Public URLs stay unchanged; a
 * filter rewrites to the internal tree before routing reaches user-scoped
 * endpoints such as {@code BlogEndpoint}.
 * <p>
 * Examples: {@link #CUSTOM_PAGE} ({@code /__custom_page__}),
 * {@link #ACTIVITY_PUB} ({@code /__activity_pub__}).
 *
 * @see dev.vepo.contraponto.custompage.CustomPageFilter
 * @see dev.vepo.contraponto.activitypub.ActivityPubIngressFilter
 */
public final class InternalRoutePrefixes {

    /**
     * Internal prefix for custom page resolution after {@code CustomPageFilter}.
     */
    public static final String CUSTOM_PAGE = "/__custom_page__";

    /**
     * Internal prefix for ActivityPub / federation endpoints after
     * {@code ActivityPubIngressFilter}.
     */
    public static final String ACTIVITY_PUB = "/__activity_pub__";

    /**
     * Builds an internal route prefix for a feature.
     *
     * @param featureSnakeCase feature slug in snake_case (e.g. {@code custom_page})
     * @return path prefix such as {@code /__custom_page__}
     */
    public static String of(String featureSnakeCase) {
        if (featureSnakeCase == null || featureSnakeCase.isBlank()) {
            throw new IllegalArgumentException("featureSnakeCase is required");
        }
        return "/__%s__".formatted(featureSnakeCase.trim());
    }

    /**
     * First path segment of an internal prefix (for reserved-segment lists).
     *
     * @param prefix value from {@link #of(String)} or a constant
     * @return segment without leading slash (e.g. {@code __custom_page__})
     */
    public static String segment(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix is required");
        }
        var normalized = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        var slash = normalized.indexOf('/');
        return slash >= 0 ? normalized.substring(0, slash) : normalized;
    }

    private InternalRoutePrefixes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
