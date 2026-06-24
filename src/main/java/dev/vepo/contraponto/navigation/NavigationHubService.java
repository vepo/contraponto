package dev.vepo.contraponto.navigation;

import java.net.URI;
import java.util.List;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@ApplicationScoped
public class NavigationHubService {

    public record HubMeta(String pageTitle, String hubTitle, String hubSubtitle) {}

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance shellPage(String pageTitle,
                                                        String hubTitle,
                                                        String hubBasePath,
                                                        BreadcrumbTrail breadcrumb,
                                                        List<HubNavGroup> navGroups,
                                                        boolean singleSectionNav,
                                                        String activeSlug,
                                                        RawString panelContent,
                                                        Links links,
                                                        LoggedUser user,
                                                        SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final NavigationHubRegistry registry;
    private final NavigationHubPanelService panelService;
    private final BreadcrumbService breadcrumbService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final SeoService seoService;

    @Inject
    public NavigationHubService(NavigationHubRegistry registry,
                                NavigationHubPanelService panelService,
                                BreadcrumbService breadcrumbService,
                                CustomPageRepository customPageRepository,
                                LoggedUser loggedUser,
                                SeoService seoService) {
        this.registry = registry;
        this.panelService = panelService;
        this.breadcrumbService = breadcrumbService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.seoService = seoService;
    }

    public String defaultSectionSlug(NavigationHub hub) {
        return registry.defaultSectionSlug(hub);
    }

    public HubMeta meta(NavigationHub hub) {
        return switch (hub) {
            case WRITING -> new HubMeta("Writing", "Writing", "Library, images, your blogs, and author appearance.");
            case READING -> new HubMeta("Reading", "Reading", "Saved posts, highlights, and notes across posts.");
            case MANAGE -> new HubMeta("Gerenciar", "Gerenciar", "Painel, páginas personalizadas, comentários e blogs da plataforma.");
            case ACCOUNT -> new HubMeta("Account", "Account", "Notifications, subscriptions, and account security.");
            case REVIEW -> new HubMeta("Review", "Review", "Editorial tools for featured posts and tags.");
            case ADMINISTRATION -> new HubMeta("Administration", "Administration", "Platform administration.");
        };
    }

    public Response redirectToDefault(NavigationHub hub) {
        URI target = UriBuilder.fromPath(registry.defaultSectionPath(hub)).build();
        return Response.seeOther(target).build();
    }

    public TemplateInstance shell(NavigationHub hub, String sectionSlug, int page) {
        return shell(hub, sectionSlug, page, false, null, null, null);
    }

    public TemplateInstance shell(NavigationHub hub,
                                  String sectionSlug,
                                  int page,
                                  boolean emailVerified,
                                  String profileError) {
        return shell(hub, sectionSlug, page, emailVerified, profileError, null, null);
    }

    public TemplateInstance shell(NavigationHub hub,
                                  String sectionSlug,
                                  int page,
                                  boolean emailVerified,
                                  String profileError,
                                  Long blogId) {
        return shell(hub, sectionSlug, page, emailVerified, profileError, blogId, null);
    }

    public TemplateInstance shell(NavigationHub hub,
                                  String sectionSlug,
                                  int page,
                                  boolean emailVerified,
                                  String profileError,
                                  Long blogId,
                                  String imageSearchQuery) {
        var section = registry.requireSection(hub, sectionSlug, loggedUser);
        var meta = meta(hub);
        var navGroups = registry.groups(hub, loggedUser);
        return Templates.shellPage(meta.pageTitle(),
                                   meta.hubTitle(),
                                   hub.path(),
                                   breadcrumbService.hubSection(hub, section.label(), section.i18nKey()),
                                   navGroups,
                                   registry.isSingleSectionHub(hub, loggedUser),
                                   sectionSlug,
                                   new RawString(panelService.render(hub,
                                                                     sectionSlug,
                                                                     page,
                                                                     emailVerified,
                                                                     profileError,
                                                                     blogId,
                                                                     imageSearchQuery)
                                                             .render()),
                                   customPageRepository.loadLinks(),
                                   loggedUser,
                                   seoService.forPrivatePage(meta.pageTitle()));
    }
}
