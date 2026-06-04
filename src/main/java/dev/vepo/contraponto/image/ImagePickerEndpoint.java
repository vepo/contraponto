package dev.vepo.contraponto.image;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@ApplicationScoped
@Path("/components/images/picker")
public class ImagePickerEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance pickerDialog(User owner,
                                                    Page<ImagePickerItem> images,
                                                    String gridBasePath,
                                                    String searchQuery,
                                                    String extraQuery);

        static native TemplateInstance pickerGrid(User owner,
                                                  Page<ImagePickerItem> images,
                                                  String gridBasePath,
                                                  String extraQuery);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    static final String GRID_BASE_PATH = "/components/images/picker/grid";

    private static String normalizeSearch(String searchQuery) {
        return searchQuery == null ? "" : searchQuery.trim();
    }

    private final ImagePickerService imagePickerService;
    private final LoggedUser loggedUser;

    private final UserRepository userRepository;

    @Inject
    public ImagePickerEndpoint(ImagePickerService imagePickerService,
                               LoggedUser loggedUser,
                               UserRepository userRepository) {
        this.imagePickerService = imagePickerService;
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
    }

    @GET
    @Path("grid")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance grid(@QueryParam("page") @DefaultValue("1") int page, @QueryParam("q") String searchQuery) {
        User owner = resolveOwner();
        Page<ImagePickerItem> images = loadPage(owner, page, searchQuery);
        return Templates.pickerGrid(owner, images, GRID_BASE_PATH, ImageControlUrls.extraQuery(searchQuery));
    }

    private Page<ImagePickerItem> loadPage(User owner, int page, String searchQuery) {
        return imagePickerService.listForOwner(owner, searchQuery, PageQuery.forGrid(ImagePickerService.PICKER_PAGE_SIZE, page));
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance picker(@QueryParam("page") @DefaultValue("1") int page, @QueryParam("q") String searchQuery) {
        User owner = resolveOwner();
        Page<ImagePickerItem> images = loadPage(owner, page, searchQuery);
        return Templates.pickerDialog(owner,
                                      images,
                                      GRID_BASE_PATH,
                                      normalizeSearch(searchQuery),
                                      ImageControlUrls.extraQuery(searchQuery));
    }

    private User resolveOwner() {
        return userRepository.findById(loggedUser.getId()).orElseThrow(NotFoundException::new);
    }
}
