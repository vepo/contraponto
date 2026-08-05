package dev.vepo.contraponto.git.legacylink;

import java.util.Optional;
import java.util.function.UnaryOperator;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.post.PostRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Resolves a blog's {@link LegacyLinkPattern} matches against posts owned by
 * the same author (invariant 3: slug unique per author across all their blogs)
 * and rewrites matched link targets to the canonical internal URL. Links whose
 * slug does not resolve to an existing post are left unchanged.
 */
@ApplicationScoped
public class LegacyLinkRewriteService {

    private final PostRepository postRepository;
    private final BlogPublicUrlService blogPublicUrlService;

    @Inject
    public LegacyLinkRewriteService(PostRepository postRepository, BlogPublicUrlService blogPublicUrlService) {
        this.postRepository = postRepository;
        this.blogPublicUrlService = blogPublicUrlService;
    }

    private Optional<String> resolveReplacement(String url, Blog blog, LegacyLinkPattern pattern) {
        return pattern.extractSlug(url)
                      .flatMap(slug -> postRepository.findByAuthorAndSlug(blog.getOwner().getId(), slug))
                      .map(blogPublicUrlService::canonicalOrPlatformAbsolute);
    }

    public String rewriteBody(String body, Blog blog, LegacyLinkPattern pattern) {
        if (body == null || body.isBlank()) {
            return body;
        }
        return LegacyLinkTargetRewriter.rewrite(body, url -> resolveReplacement(url, blog, pattern));
    }

    public UnaryOperator<String> rewriterFor(Blog blog, LegacyLinkPattern pattern) {
        return body -> rewriteBody(body, blog, pattern);
    }
}
