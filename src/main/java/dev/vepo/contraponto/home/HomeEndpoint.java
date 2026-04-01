package dev.vepo.contraponto.home;

import java.time.LocalDateTime;

import dev.vepo.contraponto.post.PostRepository;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@ApplicationScoped
public class HomeEndpoint {
    private final PostRepository postRepository;
    private final Template home;

    @Inject
    public HomeEndpoint(PostRepository postRepository, Template home) {
        this.postRepository = postRepository;
        this.home = home;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance post() {
        return home.data("posts", this.postRepository.findNewest(10))
                   .data("currentYear", LocalDateTime.now().getYear());
    }
}
