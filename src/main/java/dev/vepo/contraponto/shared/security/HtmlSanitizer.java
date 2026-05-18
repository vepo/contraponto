package dev.vepo.contraponto.shared.security;

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
                                                         .addProtocols("img", "src", "http", "https")
                                                         .addProtocols("a", "href", "http", "https", "mailto")
                                                         .preserveRelativeLinks(true);

    public String sanitizePostHtml(String html) {
        if (html == null || html.isBlank()) {
            return html == null ? "" : html;
        }
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, "", POST_CONTENT, settings);
    }
}
