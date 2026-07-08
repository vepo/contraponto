package dev.vepo.contraponto.content.render;

import dev.vepo.contraponto.image.RenderedHtmlEnricher;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostPublicationRenderedHtmlBackfill;
import dev.vepo.contraponto.renderer.Format;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class PostContentRenderer {

    private final PostContentBodyRenderer bodyRenderer;
    private final PostPublicationRenderedHtmlBackfill renderedHtmlBackfill;
    private final RenderedHtmlEnricher renderedHtmlEnricher;

    @Inject
    public PostContentRenderer(PostContentBodyRenderer bodyRenderer,
                               PostPublicationRenderedHtmlBackfill renderedHtmlBackfill,
                               RenderedHtmlEnricher renderedHtmlEnricher) {
        this.bodyRenderer = bodyRenderer;
        this.renderedHtmlEnricher = renderedHtmlEnricher;
        this.renderedHtmlBackfill = renderedHtmlBackfill;
    }

    private String enrich(String html) {
        return renderedHtmlEnricher.enrichHtml(html);
    }

    /**
     * Renders a publication snapshot: stored HTML when present, otherwise cache +
     * lazy DB backfill.
     */
    public String render(PostPublication publication) {
        if (publication == null || publication.getContent() == null || publication.getContent().trim().isEmpty()) {
            return "";
        }
        String body = publication.getRenderedHtml();
        if (body == null || body.isBlank()) {
            body = renderBody(publication.getContent(), publication.getFormat());
            renderedHtmlBackfill.backfillIfAbsent(publication, body);
        }
        return enrich(body);
    }

    /**
     * Renders draft or unpublished post content (in-memory cache only).
     */
    public String render(String content, Format format) {
        return enrich(renderBody(content, format));
    }

    /**
     * Sanitized HTML body without image alt enrichment — stored on
     * {@link PostPublication} at publish.
     */
    public String renderBody(String content, Format format) {
        return bodyRenderer.render(content, format);
    }
}
