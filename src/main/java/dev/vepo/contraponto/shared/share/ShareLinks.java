package dev.vepo.contraponto.shared.share;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ShareLinks {

    private static final String LINKEDIN_SHARE_BASE = "https://www.linkedin.com/sharing/share-offsite/?url=";
    private static final String BLUESKY_COMPOSE_BASE = "https://bsky.app/intent/compose?text=";
    private static final int BLUESKY_TEXT_LIMIT = 300;

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static ShareView from(String title, String absoluteUrl) {
        var trimmedTitle = title == null ? "" : title.trim();
        var trimmedUrl = absoluteUrl == null ? "" : absoluteUrl.trim();
        var shareText = "%s %s".formatted(trimmedTitle, trimmedUrl).trim();
        var blueskyText = truncateForBluesky(shareText);
        return new ShareView(shareText,
                             LINKEDIN_SHARE_BASE + encode(trimmedUrl),
                             BLUESKY_COMPOSE_BASE + encode(blueskyText));
    }

    private static String truncateForBluesky(String text) {
        if (text.codePointCount(0, text.length()) <= BLUESKY_TEXT_LIMIT) {
            return text;
        }
        var end = text.offsetByCodePoints(0, BLUESKY_TEXT_LIMIT);
        return text.substring(0, end);
    }

    private ShareLinks() {}
}
