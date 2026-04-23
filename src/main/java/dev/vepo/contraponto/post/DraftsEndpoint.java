package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/drafts")
@ApplicationScoped
public class DraftsEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DraftsEndpoint.class);

    @CheckedTemplate
    class Template {
        static native TemplateInstance draft(List<Post> drafts, int currentYear, LoggedUser user);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public DraftsEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance post() {
        logger.info("User info: {}", loggedUser);
        return Template.draft(this.postRepository.findDrafts(),
                              LocalDateTime.now().getYear(),
                              loggedUser);
    }
}
