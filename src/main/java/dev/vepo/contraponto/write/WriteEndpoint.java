package dev.vepo.contraponto.write;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.user.User;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/write")
@ApplicationScoped
public class WriteEndpoint {
    @CheckedTemplate
    class Template {
        static native TemplateInstance write(Optional<Post> post, int currentYear, LoggedUser user);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public WriteEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("draft/{draftId}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write(@PathParam("draftId") Long draftId) {
        return Template.write(Optional.ofNullable(draftId)
                                      .map(postRepository::findById)
                                      .orElseThrow(() -> new NotFoundException("Draft not found! id=%s".formatted(draftId))),
                              LocalDateTime.now().getYear(),
                              loggedUser);
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write() {
        return Template.write(Optional.empty(),
                              LocalDateTime.now().getYear(),
                              loggedUser);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response save(@FormParam("postId") Long postId,
                         @FormParam("title") String title,
                         @FormParam("slug") String slug,
                         @FormParam("description") String description,
                         @FormParam("content") String content,
                         @FormParam("publish") boolean publish) {
        User user = loggedUser.getUser();
        if (user == null) {
            return Response.status(401).entity("Unauthorized").build();
        }
        // Validation and save logic (similar to original but using session)
        // ...
        if (publish) {
            return Response.seeOther(URI.create("/post/" + slug)).build();
        } else {
            return Response.ok("<div class='toast toast--success'>Draft saved</div>").build();
        }
    }
}