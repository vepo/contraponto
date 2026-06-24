package dev.vepo.contraponto.readinglist;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
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
@Path("/reading/saved/components")
@ApplicationScoped
public class ReadingListHubEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(long unreadCount);

        public static native TemplateInstance tab(Page<ReadingListRow> rows,
                                                  String tab,
                                                  String basePath,
                                                  long unreadCount);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final ReadingListService readingListService;
    private final LoggedUser loggedUser;

    @Inject
    public ReadingListHubEndpoint(ReadingListService readingListService, LoggedUser loggedUser) {
        this.readingListService = readingListService;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel() {
        long unreadCount = readingListService.countUnread(loggedUser.getId());
        return Templates.panel(unreadCount);
    }

    public TemplateInstance renderTab(String tab, int page, String basePath) {
        long userId = loggedUser.getId();
        long unreadCount = readingListService.countUnread(userId);
        PageQuery query = PageQuery.forGrid(20, page);
        Page<ReadingListRow> rows = switch (tab) {
            case "unread" -> readingListService.findUnreadPage(userId, query);
            case "all" -> readingListService.findAllPage(userId, query);
            default -> throw new BadRequestException("Unknown tab: %s".formatted(tab));
        };
        return Templates.tab(rows, tab, basePath, unreadCount);
    }

    @GET
    @Path("tab/{tab}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tab(@PathParam("tab") String tab,
                                @QueryParam("page") @DefaultValue("1") int page) {
        return renderTab(tab, page, "/reading/saved");
    }
}
