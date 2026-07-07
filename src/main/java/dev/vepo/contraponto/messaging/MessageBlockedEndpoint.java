package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.LoggedUser;
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

@Logged
@Path("/account/messages/blocked")
@ApplicationScoped
public class MessageBlockedEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(Page<UserBlockRow> blocks, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final UserBlockRepository blockRepository;
    private final NavigationHubService hubService;
    private final LoggedUser loggedUser;

    @Inject
    public MessageBlockedEndpoint(UserBlockRepository blockRepository,
                                  NavigationHubService hubService,
                                  LoggedUser loggedUser) {
        this.blockRepository = blockRepository;
        this.hubService = hubService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blocked(@QueryParam("page") @DefaultValue("1") int page) {
        return hubService.shell(NavigationHub.ACCOUNT, "blocked", page);
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        Page<UserBlockRow> blocks = blockRepository.findBlockedPage(loggedUser.getId(), PageQuery.forGrid(20, page));
        return Templates.panel(blocks, basePath);
    }
}
