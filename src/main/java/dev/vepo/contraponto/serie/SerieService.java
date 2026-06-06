package dev.vepo.contraponto.serie;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Slug;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SerieService {

    private final SerieRepository serieRepository;

    @Inject
    public SerieService(SerieRepository serieRepository) {
        this.serieRepository = serieRepository;
    }

    /**
     * Clears the post's serie when {@code serieTitle} is null or blank after trim.
     * Otherwise finds or creates a serie for the post's blog from the title
     * (slugified).
     */
    @Transactional
    public void applySerieTitleToPost(Post post, String serieTitle) {
        if (serieTitle == null || serieTitle.isBlank()) {
            post.setSerie(null);
            return;
        }
        String trimmed = serieTitle.trim();
        String slug = Slug.slugify(trimmed);
        if (slug.isEmpty()) {
            post.setSerie(null);
            return;
        }
        Blog blog = post.getBlog();
        Serie serie = serieRepository.findByBlogIdAndSlug(blog.getId(), slug)
                                     .orElseGet(() -> serieRepository.persist(new Serie(blog, trimmed, slug)));
        serie.setTitle(trimmed);
        post.setSerie(serie);
    }
}
