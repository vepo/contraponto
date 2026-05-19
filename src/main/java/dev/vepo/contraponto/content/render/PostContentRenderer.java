package dev.vepo.contraponto.content.render;

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

    private final ContentRenderTagProcessor tagProcessor;
    private final HtmlSanitizer htmlSanitizer;
    private final RenderedHtmlEnricher renderedHtmlEnricher;

    @Inject
    public PostContentRenderer(ContentRenderTagProcessor tagProcessor,
                               HtmlSanitizer htmlSanitizer,
                               RenderedHtmlEnricher renderedHtmlEnricher) {
        this.tagProcessor = tagProcessor;
        this.htmlSanitizer = htmlSanitizer;
        this.renderedHtmlEnricher = renderedHtmlEnricher;
    }

    public String render(String content, Format format) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String withEmbeds = tagProcessor.apply(content);
        String html = Renderer.get(format).render(withEmbeds);
        html = htmlSanitizer.sanitizePostHtml(html);
        return renderedHtmlEnricher.enrichHtml(html);
    }
}
