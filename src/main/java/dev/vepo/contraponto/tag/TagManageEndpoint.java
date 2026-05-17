package dev.vepo.contraponto.tag;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/tags/manage")
@ApplicationScoped
public class TagManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance list(Page<TagRow> tags, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final TagRepository tagRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public TagManageEndpoint(TagRepository tagRepository,
                             CustomPageRepository customPageRepository,
                             LoggedUser loggedUser) {
        this.tagRepository = tagRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You do not have permission to manage tags.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response list(@QueryParam("page") @DefaultValue("1") int page) {
        if (!loggedUser.isEditor()) {
            return forbidden();
        }

        return Response.ok(Templates.list(listPage(page), customPageRepository.loadLinks(), loggedUser)).build();
    }

    public Page<TagRow> listPage(int page) {
        return tagRepository.findPageForManagement(PageQuery.forGrid(20, page)).map(TagRow::from);
    }

    TemplateInstance renderList() {
        return Templates.list(listPage(1), customPageRepository.loadLinks(), loggedUser);
    }
}
