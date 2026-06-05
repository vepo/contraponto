package dev.vepo.contraponto.seo;

import java.util.List;

public final class CrawlerPrivatePaths {

    private static final List<String> PREFIXES = List.of("/write",
                                                         "/writing",
                                                         "/reading",
                                                         "/highlights",
                                                         "/manage",
                                                         "/account",
                                                         "/administration",
                                                         "/editor",
                                                         "/forms/",
                                                         "/api/",
                                                         "/auth/",
                                                         "/blogs",
                                                         "/users",
                                                         "/library",
                                                         "/dashboard",
                                                         "/profile",
                                                         "/review",
                                                         "/pages",
                                                         "/comments",
                                                         "/notifications",
                                                         "/subscriptions",
                                                         "/password-recovery",
                                                         "/_custom_page",
                                                         "/components/",
                                                         "/search");

    private static final List<String> EXACT = List.of("/feed");

    public static List<String> disallowRules() {
        return PREFIXES;
    }

    public static boolean isPrivatePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (EXACT.contains(path)) {
            return true;
        }
        return PREFIXES.stream().anyMatch(path::startsWith);
    }

    private CrawlerPrivatePaths() {
        throw new UnsupportedOperationException("Utility class");
    }
}
