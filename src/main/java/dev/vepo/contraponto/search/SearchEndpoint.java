package dev.vepo.contraponto.search;

import java.util.List;
import java.util.stream.Collectors;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchEndpoint {

    @Inject
    EntityManager entityManager;

    @Inject
    PostRepository postRepository;

    @GET
    public Response search(@QueryParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return Response.ok(List.of()).build();
        }

        String searchPattern = "%" + query.toLowerCase() + "%";

        List<Post> posts = entityManager.createQuery("""
                                                     FROM Post p
                                                     WHERE p.published = TRUE
                                                     AND (LOWER(p.title) LIKE :pattern
                                                         OR LOWER(p.content) LIKE :pattern
                                                         OR LOWER(p.description) LIKE :pattern)
                                                     ORDER BY p.publishedAt DESC
                                                     """, Post.class)
                                        .setParameter("pattern", searchPattern)
                                        .setMaxResults(20)
                                        .getResultList();

        List<SearchResult> results = posts.stream()
                                          .map(post -> new SearchResult(post.getId(),
                                                                        post.getSlug(),
                                                                        post.getTitle(),
                                                                        post.getDescription(),
                                                                        post.getContent() != null
                                                                                && post.getContent().length() > 200
                                                                                                                    ? post.getContent().substring(0, 200)
                                                                                                                            + "..."
                                                                                                                    : post.getContent(),
                                                                        post.getAuthor(),
                                                                        post.getPublishedAt(),
                                                                        post.getCover() != null ? new CoverInfo(post.getCover().getUrl()) : null))
                                          .collect(Collectors.toList());

        return Response.ok(results).build();
    }

    public record SearchResult(Long id,
                               String slug,
                               String title,
                               String description,
                               String excerpt,
                               String author,
                               java.time.LocalDateTime publishedAt,
                               CoverInfo cover) {}

    public record CoverInfo(String url) {}
}