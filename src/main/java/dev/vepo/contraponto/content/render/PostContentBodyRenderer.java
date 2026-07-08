package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.image.ContentImageMarkerService;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.renderer.Renderer;
import dev.vepo.contraponto.shared.security.HtmlSanitizer;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Renders post body to sanitized HTML (markers, plugins, AsciiDoc/Markdown,
 * sanitizer). Cached in memory; does not apply per-request image alt
 * enrichment.
 */
@ApplicationScoped
public class PostContentBodyRenderer {

    private final ContentImageMarkerService contentImageMarkerService;
    private final ContentRenderTagProcessor tagProcessor;
    private final HtmlSanitizer htmlSanitizer;

    @Inject
    public PostContentBodyRenderer(ContentImageMarkerService contentImageMarkerService,
                                   ContentRenderTagProcessor tagProcessor,
                                   HtmlSanitizer htmlSanitizer) {
        this.contentImageMarkerService = contentImageMarkerService;
        this.tagProcessor = tagProcessor;
        this.htmlSanitizer = htmlSanitizer;
    }

    @CacheResult(cacheName = "post-content-body")
    public String render(String content, Format format) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String withoutMarkers = contentImageMarkerService.toEditorContent(content)
                                                         .replace("//api/images/", "/api/images/");
        String html = format == Format.ASCIIDOC
                                                ? Renderer.get(format).render(tagProcessor.applyWithAsciiDocPassthrough(withoutMarkers))
                                                : Renderer.get(format).render(tagProcessor.apply(withoutMarkers));
        return htmlSanitizer.sanitizePostHtml(html);
    }
}
