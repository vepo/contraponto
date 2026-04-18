package dev.vepo.contraponto.post;

import java.time.LocalDateTime;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
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
    @CheckedTemplate
    class Template {
        static native TemplateInstance post(Post post, int currentYear, LoggedUser user);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public PostEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance post(@PathParam("id") String slug) {
        return Template.post(this.postRepository.findBySlug(slug)
                                                .orElseThrow(() -> new NotFoundException("Post not found! slug=%s".formatted(slug))),
                             LocalDateTime.now().getYear(),
                             loggedUser);
    }
}