package dev.vepo.contraponto.directory;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
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
        public static native TemplateInstance authors(java.util.List<User> authors,
                                                      Links links,
                                                      LoggedUser user,
                                                      SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final UserRepository userRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final SeoService seoService;

    @Inject
    public AuthorDirectoryEndpoint(UserRepository userRepository,
                                   CustomPageRepository customPageRepository,
                                   LoggedUser loggedUser,
                                   SeoService seoService) {
        this.userRepository = userRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.seoService = seoService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance authors() {
        return Templates.authors(userRepository.findAuthorsWithPublishedPosts(),
                                 customPageRepository.loadLinks(),
                                 loggedUser,
                                 seoService.forAuthorDirectory());
    }
}
