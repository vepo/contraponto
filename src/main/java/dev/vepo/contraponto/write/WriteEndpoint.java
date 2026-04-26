package dev.vepo.contraponto.write;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
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

@Logged
@Path("/write")
@ApplicationScoped
public class WriteEndpoint {
    @CheckedTemplate
    @SuppressWarnings("java:S1118")
    public static class Templates {
        public static native TemplateInstance write(Optional<Post> post, LoggedUser user);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public WriteEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write() {
        return Templates.write(Optional.empty(),
                               loggedUser);
    }

    @GET
    @Path("draft/{draftId}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write(@PathParam("draftId") Long draftId) {
        var maybePost = Optional.ofNullable(draftId)
                                .flatMap(postRepository::findById)
                                .filter(post -> Objects.equals(post.getAuthor().getId(), loggedUser.getId()));
        if (maybePost.isEmpty()) {
            throw new NotFoundException("Draft not found! id=%s".formatted(draftId));
        }
        return Templates.write(maybePost, loggedUser);
    }
}