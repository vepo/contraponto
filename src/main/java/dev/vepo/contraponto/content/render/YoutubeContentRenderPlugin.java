package dev.vepo.contraponto.content.render;

import java.util.List;
import java.util.regex.Pattern;

public final class YoutubeContentRenderPlugin implements ContentRenderPlugin {

    private static final Pattern VIDEO_ID = Pattern.compile("^[a-zA-Z0-9_-]{11}$");

    private static String error(String message) {
        return "<p class=\"content-render content-render--error\">%s</p>".formatted(escape(message));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    @Override
    public String identifier() {
        return "youtube";
    }

    @Override
    public String render(List<String> params) {
        if (params == null || params.isEmpty()) {
            return error("YouTube video id required.");
        }
        String id = params.getFirst().trim();
        if (!VIDEO_ID.matcher(id).matches()) {
            return error("Invalid YouTube video id.");
        }
        return """
               <iframe width="560" height="315" src="https://www.youtube.com/embed/%s" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
               """.formatted(id)
                  .trim();
    }
}
