package dev.vepo.contraponto.shared.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Strips scripts and dangerous markup from author-supplied HTML before
 * rendering or storage.
 */
@ApplicationScoped
public class HtmlSanitizer {

    private static final Safelist POST_CONTENT = Safelist.relaxed()
                                                         .addTags("details", "summary", "figure", "figcaption", "iframe", "script")
                                                         .addAttributes(":all", "class")
                                                         .addAttributes("iframe", "src", "title", "width", "height", "frameborder", "allow", "allowfullscreen",
                                                                        "loading", "referrerpolicy")
                                                         .addAttributes("script", "src")
                                                         .addProtocols("a", "href", "http", "https", "mailto")
                                                         .addProtocols("iframe", "src", "https")
                                                         .addProtocols("script", "src", "https")
                                                         .preserveRelativeLinks(true);

    private static final Safelist BLOG_DESCRIPTION = Safelist.basic()
                                                             .addTags("h2", "h3", "h4", "blockquote", "code", "pre")
                                                             .addAttributes(":all", "class")
                                                             .addProtocols("a", "href", "http", "https", "mailto");

    private static String stripDisallowedEmbeds(String html) {
        var doc = Jsoup.parseBodyFragment(html);
        doc.select("iframe").forEach(iframe -> {
            String src = iframe.attr("src");
            if (!src.startsWith("https://www.youtube.com/embed/")
                    && !src.startsWith("https://www.youtube-nocookie.com/embed/")) {
                iframe.remove();
            }
        });
        doc.select("script[src]").forEach(script -> {
            String src = script.attr("src");
            if (!src.startsWith("https://gist.github.com/")) {
                script.remove();
            }
        });
        doc.select("script:not([src])").remove();
        return doc.body().html();
    }

    private final String baseUrl;

    public HtmlSanitizer(@ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String sanitizeBlogDescriptionHtml(String html) {
        if (html == null || html.isBlank()) {
            return html == null ? "" : html;
        }
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, baseUrl, BLOG_DESCRIPTION, settings);
    }

    public String sanitizePostHtml(String html) {
        if (html == null || html.isBlank()) {
            return html == null ? "" : html;
        }
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(html, baseUrl, POST_CONTENT, settings);
        return stripDisallowedEmbeds(cleaned);
    }
}
