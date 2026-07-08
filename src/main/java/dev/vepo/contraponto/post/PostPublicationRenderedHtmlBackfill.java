package dev.vepo.contraponto.post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Persists lazily computed {@link PostPublication#getRenderedHtml()} for rows
 * created before publish-time rendering (e.g. dev-import SQL).
 */
@ApplicationScoped
public class PostPublicationRenderedHtmlBackfill {

    private final PostPublicationRepository publicationRepository;

    @Inject
    public PostPublicationRenderedHtmlBackfill(PostPublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    @Transactional
    public void backfillIfAbsent(PostPublication publication, String renderedHtml) {
        if (publication == null || publication.getId() == null || renderedHtml == null || renderedHtml.isBlank()) {
            return;
        }
        if (publication.getRenderedHtml() != null && !publication.getRenderedHtml().isBlank()) {
            return;
        }
        if (publicationRepository.updateRenderedHtmlIfNull(publication.getId(), renderedHtml)) {
            publication.setRenderedHtml(renderedHtml);
        }
    }
}
