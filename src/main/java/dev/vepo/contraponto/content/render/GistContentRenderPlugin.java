package dev.vepo.contraponto.content.render;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

public final class GistContentRenderPlugin implements ContentRenderPlugin {

    private static final Pattern GIST_PATH = Pattern.compile("^/([a-zA-Z0-9_-]+)/([a-f0-9]+)$");

    private static String error(String message) {
        return "<p class=\"content-render content-render--error\">" + escape(message) + "</p>";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    @Override
    public String identifier() {
        return "gist";
    }

    @Override
    public String render(List<String> params) {
        if (params == null || params.isEmpty()) {
            return error("Gist URL required.");
        }
        String url = String.join(" ", params).trim();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return error("Invalid Gist URL.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"gist.github.com".equalsIgnoreCase(uri.getHost())) {
            return error("Gist URL must be https://gist.github.com/{user}/{id}.");
        }
        var matcher = GIST_PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            return error("Invalid Gist URL.");
        }
        String scriptSrc = "https://gist.github.com/" + matcher.group(1) + "/" + matcher.group(2) + ".js";
        return """
               <div class="content-render content-render--gist">
               <script src="%s"></script>
               </div>
               """.formatted(scriptSrc);
    }
}
