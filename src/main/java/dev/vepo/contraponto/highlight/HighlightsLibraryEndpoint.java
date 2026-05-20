package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
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

@Logged
@Path("/highlights")
@ApplicationScoped
public class HighlightsLibraryEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance library(dev.vepo.contraponto.shared.pagination.Page<HighlightLibraryRow> highlights,
                                                      Links links,
                                                      LoggedUser user,
                                                      BreadcrumbTrail breadcrumb);

        private Templates() {
            throw new UnsupportedOperationException("Utility class");
        }
    }

    private final PostTextHighlightRepository highlightRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public HighlightsLibraryEndpoint(PostTextHighlightRepository highlightRepository,
                                     CustomPageRepository customPageRepository,
                                     LoggedUser loggedUser,
                                     BreadcrumbService breadcrumbService) {
        this.highlightRepository = highlightRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance library(@QueryParam("page") @DefaultValue("1") int page) {
        var highlights = highlightRepository.findLibraryForUser(loggedUser.getId(), PageQuery.forGrid(20, page));
        Links links = customPageRepository.loadLinks();
        return Templates.library(highlights, links, loggedUser, breadcrumbService.forHighlightsLibrary());
    }
}
