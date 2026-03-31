package dev.vepo.contraponto.post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("post/{id}")
@ApplicationScoped
public class PostEndpoint {

    private final PostRepository postRepository;

    @Inject
    public PostEndpoint(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String post(@PathParam("id") String slug) {
        return this.postRepository.findBySlug(slug)
                                  .map(Post::toString)
                                  .orElseThrow(() -> new NotFoundException("Post not found! slug=%s".formatted(slug)));
    }
}
