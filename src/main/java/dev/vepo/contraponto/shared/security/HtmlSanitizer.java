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
                                                         .addTags("details", "summary", "figure", "figcaption")
                                                         .addAttributes(":all", "class")
                                                         .addProtocols("a", "href", "http", "https", "mailto")
                                                         .preserveRelativeLinks(true);

    private final String baseUrl;

    public HtmlSanitizer(@ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String sanitizePostHtml(String html) {
        if (html == null || html.isBlank()) {
            return html == null ? "" : html;
        }
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, baseUrl, POST_CONTENT, settings);
    }
}
