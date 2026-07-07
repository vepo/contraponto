package dev.vepo.contraponto.messaging;

import java.util.List;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
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
@ApplicationScoped
@Path("/components/messages/recipient-suggestions")
public class MessageRecipientSuggestionComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance options(List<RecipientSuggestion> suggestions);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final MessageComposeService composeService;
    private final LoggedUser loggedUser;

    @Inject
    public MessageRecipientSuggestionComponentEndpoint(MessageComposeService composeService, LoggedUser loggedUser) {
        this.composeService = composeService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance suggestions(@QueryParam("to") String usernamePrefix) {
        if (!loggedUser.isAuthenticated()) {
            throw new NotFoundException();
        }
        return Templates.options(composeService.suggestRecipients(loggedUser.getId(), usernamePrefix));
    }
}
