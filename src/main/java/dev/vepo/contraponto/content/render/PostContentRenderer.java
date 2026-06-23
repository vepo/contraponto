package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.image.ContentImageMarkerService;
import dev.vepo.contraponto.image.RenderedHtmlEnricher;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.renderer.Renderer;
import dev.vepo.contraponto.shared.security.HtmlSanitizer;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class PostContentRenderer {

    private final ContentImageMarkerService contentImageMarkerService;
    private final ContentRenderTagProcessor tagProcessor;
    private final HtmlSanitizer htmlSanitizer;
    private final RenderedHtmlEnricher renderedHtmlEnricher;

    @Inject
    public PostContentRenderer(ContentImageMarkerService contentImageMarkerService,
                               ContentRenderTagProcessor tagProcessor,
                               HtmlSanitizer htmlSanitizer,
                               RenderedHtmlEnricher renderedHtmlEnricher) {
        this.contentImageMarkerService = contentImageMarkerService;
        this.tagProcessor = tagProcessor;
        this.htmlSanitizer = htmlSanitizer;
        this.renderedHtmlEnricher = renderedHtmlEnricher;
    }

    public String render(String content, Format format) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String withoutMarkers = contentImageMarkerService.toEditorContent(content)
                                                         .replace("//api/images/", "/api/images/");
        // AsciiDoc safe mode escapes raw HTML, so render tags must be expanded after
        // conversion.
        // Markdown allows inline HTML, so tags are expanded first for clean block embed
        // markup.
        String html = format == Format.ASCIIDOC
                                                ? tagProcessor.apply(Renderer.get(format).render(withoutMarkers))
                                                : Renderer.get(format).render(tagProcessor.apply(withoutMarkers));
        html = htmlSanitizer.sanitizePostHtml(html);
        return renderedHtmlEnricher.enrichHtml(html);
    }
}
