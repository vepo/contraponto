package dev.vepo.contraponto.rss;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;

public final class RssFeedRenderer {

    public record Channel(String title, String linkPath, String description) {}

    private static final DateTimeFormatter RFC1123 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private static void appendElement(StringBuilder sb, String name, String body) {
        sb.append('<').append(name).append('>');
        sb.append(body);
        sb.append("</").append(name).append('>');
    }

    static String escapeXml(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }

    private static String formatRfc1123(LocalDateTime time) {
        ZonedDateTime zdt = time.atZone(ZoneOffset.UTC);
        return RFC1123.format(zdt);
    }

    static URI itemUri(URI baseUri, Post post) {
        return resolve(baseUri, PostEndpoint.extractUrl(post));
    }

    private static String liveTitle(Post post, PostPublication live) {
        if (live != null && live.getTitle() != null) {
            return live.getTitle();
        }
        return post.getTitle();
    }

    private static LocalDateTime newestTimestamp(List<Post> posts) {
        LocalDateTime max = null;
        for (Post p : posts) {
            PostPublication live = p.getLivePublication();
            LocalDateTime candidate = publicationTimestamp(p, live);
            if (candidate != null && (max == null || candidate.isAfter(max))) {
                max = candidate;
            }
        }
        return max;
    }

    private static LocalDateTime publicationTimestamp(Post post, PostPublication live) {
        if (live != null) {
            return live.getPublishedAt();
        }
        if (post.getPublishedAt() != null) {
            return post.getPublishedAt();
        }
        return post.getUpdatedAt();
    }

    public static String render(Channel channel, List<Post> posts, URI baseUri) {
        URI channelLink = resolve(baseUri, channel.linkPath());
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\"><channel>");
        appendElement(sb, "title", escapeXml(channel.title()));
        appendElement(sb, "link", escapeXml(channelLink.toString()));
        appendElement(sb, "description", escapeXml(channel.description()));
        appendElement(sb, "language", "en-us");

        LocalDateTime newest = newestTimestamp(posts);
        appendElement(sb,
                      "lastBuildDate",
                      formatRfc1123(newest != null ? newest : LocalDateTime.now(ZoneOffset.UTC)));

        for (Post post : posts) {
            URI itemLink = itemUri(baseUri, post);
            PostPublication live = post.getLivePublication();
            LocalDateTime pub = publicationTimestamp(post, live);
            String title = liveTitle(post, live);
            sb.append("<item>");
            appendElement(sb, "title", escapeXml(title));
            appendElement(sb, "link", escapeXml(itemLink.toString()));
            sb.append("<guid isPermaLink=\"true\">").append(escapeXml(itemLink.toString())).append("</guid>");
            appendElement(sb, "pubDate", formatRfc1123(pub != null ? pub : post.getCreatedAt()));
            String summary = live != null ? live.getDescription() : post.getDescription();
            if (summary == null || summary.isBlank()) {
                summary = title;
            }
            appendElement(sb, "description", escapeXml(summary));
            sb.append("</item>");
        }

        sb.append("</channel></rss>");
        return sb.toString();
    }

    private static URI resolve(URI baseUri, String pathStartingWithSlash) {
        String relative = pathStartingWithSlash.startsWith("/") ? pathStartingWithSlash.substring(1) : pathStartingWithSlash;
        return baseUri.resolve(relative);
    }

    private RssFeedRenderer() {
        throw new UnsupportedOperationException("Utility class");
    }
}
