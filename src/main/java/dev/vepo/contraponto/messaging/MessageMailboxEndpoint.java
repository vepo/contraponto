package dev.vepo.contraponto.messaging;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/account/messages/components/tab")
@ApplicationScoped
public class MessageMailboxEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(String basePath);

        public static native TemplateInstance tab(Page<MessageThreadRow> rows,
                                                  String tab,
                                                  String basePath,
                                                  String extraQuery);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final MessageThreadRepository threadRepository;
    private final LoggedUser loggedUser;

    @Inject
    public MessageMailboxEndpoint(MessageThreadRepository threadRepository, LoggedUser loggedUser) {
        this.threadRepository = threadRepository;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel(String basePath) {
        return Templates.panel(basePath);
    }

    public TemplateInstance renderTab(String tab, int page, String basePath) {
        long userId = loggedUser.getId();
        PageQuery query = PageQuery.forGrid(20, page);
        MessageThreadStatus status = switch (tab) {
            case "open" -> MessageThreadStatus.OPEN;
            case "closed" -> MessageThreadStatus.CLOSED;
            default -> throw new BadRequestException("Unknown tab: %s".formatted(tab));
        };
        Page<MessageThreadRow> rows = threadRepository.findMailboxPage(userId, status, query);
        return Templates.tab(rows, tab, basePath, "tab=%s".formatted(tab));
    }

    @GET
    @Path("{tab}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tab(@PathParam("tab") String tab,
                                @QueryParam("page") @DefaultValue("1") int page) {
        return renderTab(tab, page, MessageThreadPaths.mailbox());
    }
}
