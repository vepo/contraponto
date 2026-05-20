package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Logged
@Path("/highlights")
@ApplicationScoped
public class HighlightsLibraryEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance highlightsPanel(Page<HighlightLibraryEntry> highlights, String basePath);

        public static native TemplateInstance notesPanel(Page<HighlightNoteLibraryRow> notes, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final ReadingLibraryService readingLibraryService;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightsLibraryEndpoint(ReadingLibraryService readingLibraryService, LoggedUser loggedUser) {
        this.readingLibraryService = readingLibraryService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response legacyLibrary(@QueryParam("page") @DefaultValue("1") int page) {
        var target = UriBuilder.fromPath("/reading/highlights");
        if (page > 1) {
            target.queryParam("page", page);
        }
        return Response.seeOther(target.build()).build();
    }

    public TemplateInstance renderHighlightsHubPanel(int page, String basePath) {
        var highlights = readingLibraryService.findHighlightsPage(loggedUser.getId(), PageQuery.forGrid(20, page));
        return Templates.highlightsPanel(highlights, basePath);
    }

    public TemplateInstance renderNotesHubPanel(int page, String basePath) {
        var notes = readingLibraryService.findNotesPage(loggedUser.getId(), PageQuery.forGrid(20, page));
        return Templates.notesPanel(notes, basePath);
    }
}
