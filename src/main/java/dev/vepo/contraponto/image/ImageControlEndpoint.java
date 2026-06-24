package dev.vepo.contraponto.image;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@ApplicationScoped
public class ImageControlEndpoint {

    @Path("/blogs/{blogId}/images")
    public static class LegacyBlogImagesRedirect {

        private LegacyBlogImagesRedirect() {
            throw new UnsupportedOperationException("Utility class");
        }

        @GET
        @Operation(hidden = true)
        public Response redirect(@SuppressWarnings("unused") @PathParam("blogId") long blogId,
                                 @QueryParam("page") @DefaultValue("1") int page,
                                 @QueryParam("q") String searchQuery) {
            var builder = UriBuilder.fromPath(ImageControlUrls.HUB_PATH).queryParam("page", page);
            if (searchQuery != null && !searchQuery.isBlank()) {
                builder.queryParam("q", searchQuery.trim());
            }
            return Response.seeOther(builder.build()).build();
        }
    }

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance panel(User owner,
                                             Page<ImageControlRow> images,
                                             String searchQuery,
                                             String extraQuery);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private static String normalizeSearch(String searchQuery) {
        return searchQuery == null ? "" : searchQuery.trim();
    }

    private final ImageControlService imageControlService;
    private final LoggedUser loggedUser;

    private final UserRepository userRepository;

    @Inject
    public ImageControlEndpoint(ImageControlService imageControlService,
                                LoggedUser loggedUser,
                                UserRepository userRepository) {
        this.imageControlService = imageControlService;
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
    }

    public Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    public TemplateInstance renderHubPanel(int page, String searchQuery) {
        User owner = userRepository.findById(loggedUser.getId()).orElseThrow();
        Page<ImageControlRow> images = imageControlService.listForOwner(owner, searchQuery, PageQuery.forGrid(20, page));
        return Templates.panel(owner, images, normalizeSearch(searchQuery), ImageControlUrls.extraQuery(searchQuery));
    }
}
