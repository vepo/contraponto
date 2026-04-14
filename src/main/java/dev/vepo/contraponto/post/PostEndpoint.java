package dev.vepo.contraponto.post;

import java.time.LocalDateTime;

import dev.vepo.contraponto.shared.infra.UserContext.UserInfo;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
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
    private final Template post;
    private final UserInfo userInfo;

    @Inject
    public PostEndpoint(PostRepository postRepository, Template post, UserInfo userInfo) {
        this.postRepository = postRepository;
        this.post = post;
        this.userInfo = userInfo;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance post(@PathParam("id") String slug) {
        return post.data("post", this.postRepository.findBySlug(slug)
                                                    .orElseThrow(() -> new NotFoundException("Post not found! slug=%s".formatted(slug))))
                   .data("currentYear", LocalDateTime.now().getYear())
                   .data("user", userInfo);
    }
}