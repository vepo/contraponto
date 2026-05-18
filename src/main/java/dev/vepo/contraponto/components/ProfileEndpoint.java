package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
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

@Logged
@Path("/profile")
@ApplicationScoped
public class ProfileEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance profile(Links links, User user, long mainBlogId, LoggedUser loggedUser);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ProfileEndpoint.class);

    private final LoggedUser loggedUser;
    private final CustomPageRepository customPageRepository;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;

    @Inject
    public ProfileEndpoint(CustomPageRepository customPageRepository,
                           UserRepository userRepository,
                           BlogRepository blogRepository,
                           LoggedUser loggedUser) {
        this.customPageRepository = customPageRepository;
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profile() {
        logger.info("Reloading meny...");
        var user = userRepository.findById(loggedUser.getId()).orElseThrow();
        var mainBlogId = blogRepository.findMainByOwnerId(user.getId()).map(b -> b.getId()).orElse(0L);
        return Templates.profile(customPageRepository.loadLinks(), user, mainBlogId, loggedUser);
    }
}