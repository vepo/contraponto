package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/account/messages/compose")
@ApplicationScoped
public class MessageComposeEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(User recipient, String recipientUsername);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final UserRepository userRepository;
    private final UserBlockRepository blockRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final NavigationHubService hubService;
    private final SeoService seoService;

    @Inject
    public MessageComposeEndpoint(UserRepository userRepository,
                                  UserBlockRepository blockRepository,
                                  LoggedUser loggedUser,
                                  BreadcrumbService breadcrumbService,
                                  NavigationHubService hubService,
                                  SeoService seoService) {
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.hubService = hubService;
        this.seoService = seoService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance compose(@QueryParam("to") String toUsername) {
        User recipient = null;
        if (toUsername != null && !toUsername.isBlank()) {
            recipient = userRepository.findActiveByUsername(toUsername.trim())
                                      .orElseThrow(() -> new NotFoundException("User not found."));
            if (blockRepository.isBlockedEitherDirection(loggedUser.getId(), recipient.getId())) {
                throw new NotFoundException("User not found.");
            }
        }
        var breadcrumb = breadcrumbService.forMessageCompose();
        return hubService.shellWithCustomPanel(NavigationHub.ACCOUNT,
                                               "mailbox",
                                               breadcrumb,
                                               new RawString(Templates.panel(recipient, toUsername).render()),
                                               seoService.forPrivatePage("Compose message"));
    }
}
