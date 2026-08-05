package dev.vepo.contraponto.reddit.account;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;

/**
 * Owns the Appearance-panel Reddit account section fragment and its Disconnect
 * mutation.
 */
@Logged
@ApplicationScoped
@Path("/forms/writing/reddit")
public class RedditAccountEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance section(boolean configured, boolean connected, String redditUsername);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final RedditAppearanceService appearanceService;
    private final RedditAccountLinkService linkService;

    @Inject
    public RedditAccountEndpoint(LoggedUser loggedUser,
                                 UserRepository userRepository,
                                 RedditAppearanceService appearanceService,
                                 RedditAccountLinkService linkService) {
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
        this.appearanceService = appearanceService;
        this.linkService = linkService;
    }

    @POST
    @Path("disconnect")
    @Produces(MediaType.TEXT_HTML)
    public Response disconnect() {
        if (!loggedUser.isAuthenticated()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var user = userRepository.findById(loggedUser.getId()).orElseThrow();
        linkService.unlink(user);
        return Response.ok(renderSection(user)).build();
    }

    public TemplateInstance renderSection(User user) {
        var view = appearanceService.buildView(user);
        return Templates.section(view.configured(), view.connected(), view.redditUsername());
    }
}
