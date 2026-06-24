package dev.vepo.contraponto.library;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/writing/library/components")
@ApplicationScoped
public class LibraryEndpoint {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance panel();

        public static native TemplateInstance tab(Page<Post> posts, String type);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;

    @Inject
    public LibraryEndpoint(PostRepository postRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel() {
        return Templates.panel();
    }

    @GET
    @Path("tab/{type}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tab(@PathParam("type") String type, @QueryParam("page") @DefaultValue("1") int page) {
        return Templates.tab(switch (type) {
            case "published" -> postRepository.findPageByAuthorAndPublished(loggedUser.getId(), true, PageQuery.forGrid(20, page));
            case "drafts" -> postRepository.findPageByAuthorAndPublished(loggedUser.getId(), false, PageQuery.forGrid(20, page));
            default -> throw new BadRequestException("Type not defined! type=%s".formatted(type));
        }, type);
    }
}
