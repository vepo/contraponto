package dev.vepo.contraponto.directory;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/authors")
@ApplicationScoped
public class AuthorDirectoryEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance authors(java.util.List<AuthorDirectoryRow> rows,
                                                      Links links,
                                                      LoggedUser user,
                                                      BreadcrumbTrail breadcrumb,
                                                      SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final AuthorDirectoryService authorDirectoryService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final SeoService seoService;

    @Inject
    public AuthorDirectoryEndpoint(AuthorDirectoryService authorDirectoryService,
                                   CustomPageRepository customPageRepository,
                                   LoggedUser loggedUser,
                                   BreadcrumbService breadcrumbService,
                                   SeoService seoService) {
        this.authorDirectoryService = authorDirectoryService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.seoService = seoService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance authors() {
        return Templates.authors(authorDirectoryService.buildRows(),
                                 customPageRepository.loadLinks(),
                                 loggedUser,
                                 breadcrumbService.forAuthorDirectory(),
                                 seoService.forAuthorDirectory());
    }
}
