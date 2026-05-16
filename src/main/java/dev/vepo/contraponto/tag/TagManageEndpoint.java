package dev.vepo.contraponto.tag;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/tags/manage")
@ApplicationScoped
public class TagManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance list(List<TagRow> tags, Links links, LoggedUser user);

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
    public Response list() {
        if (!loggedUser.isEditor()) {
            return forbidden();
        }

        var tags = tagRepository.listAllForManagement().stream().map(TagRow::from).toList();
        return Response.ok(Templates.list(tags, customPageRepository.loadLinks(), loggedUser)).build();
    }

    TemplateInstance renderList() {
        var tags = tagRepository.listAllForManagement().stream().map(TagRow::from).toList();
        return Templates.list(tags, customPageRepository.loadLinks(), loggedUser);
    }
}
