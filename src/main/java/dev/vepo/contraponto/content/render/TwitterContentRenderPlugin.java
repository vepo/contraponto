package dev.vepo.contraponto.content.render;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class TwitterContentRenderPlugin implements ContentRenderPlugin {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
                                                            "twitter.com",
                                                            "www.twitter.com",
                                                            "mobile.twitter.com",
                                                            "x.com",
                                                            "www.x.com",
                                                            "mobile.x.com");

    private static final Pattern STATUS_PATH = Pattern.compile("^/([^/]+)/status/([0-9]+)/?$");

    private static String error(String message) {
        return "<p class=\"content-render content-render--error\">%s</p>".formatted(escape(message));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    static Optional<String> parseTweetUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }
        String host = uri.getHost();
        if (host == null || ALLOWED_HOSTS.stream().noneMatch(allowed -> allowed.equalsIgnoreCase(host))) {
            return Optional.empty();
        }
        var matcher = STATUS_PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String username = matcher.group(1);
        String statusId = matcher.group(2);
        return Optional.of("https://twitter.com/%s/status/%s".formatted(username, statusId));
    }

    @Override
    public String identifier() {
        return "twitter";
    }

    @Override
    public String render(List<String> params) {
        if (params == null || params.isEmpty()) {
            return error("Twitter post URL required.");
        }
        String url = String.join(" ", params).trim();
        Optional<String> canonicalUrl = parseTweetUrl(url);
        if (canonicalUrl.isEmpty()) {
            return error("Twitter post URL must be https://twitter.com/{user}/status/{id} or https://x.com/{user}/status/{id}.");
        }
        return """
               <div class="content-render content-render--twitter">
               <blockquote class="twitter-tweet"><a href="%1$s"></a></blockquote>
               <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>
               </div>
               """.formatted(canonicalUrl.get());
    }
}
