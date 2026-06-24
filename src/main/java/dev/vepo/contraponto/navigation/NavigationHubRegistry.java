package dev.vepo.contraponto.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class NavigationHubRegistry {

    private List<HubNavGroup> accountGroups() {
        return List.of(new HubNavGroup("Activity",
                                       List.of(new HubSectionNav("notifications", "Notifications"),
                                               new HubSectionNav("subscriptions", "Subscriptions"))),
                       new HubNavGroup("Account", List.of(new HubSectionNav("security", "Security"))));
    }

    private List<HubNavGroup> administrationGroups() {
        return List.of(new HubNavGroup("Platform",
                                       List.of(new HubSectionNav("users", "Users"),
                                               new HubSectionNav("insights", "Platform insights", "administration.nav.insights"))));
    }

    public String defaultSectionPath(NavigationHub hub) {
        return sectionPath(hub, defaultSectionSlug(hub));
    }

    public String defaultSectionSlug(NavigationHub hub) {
        return switch (hub) {
            case WRITING -> "library";
            case READING -> "highlights";
            case MANAGE -> "dashboard";
            case ACCOUNT -> "notifications";
            case REVIEW -> "review";
            case ADMINISTRATION -> "users";
        };
    }

    private List<HubNavGroup> editorGroups() {
        return List.of(new HubNavGroup("Editorial",
                                       List.of(new HubSectionNav("review", "Featured Posts"),
                                               new HubSectionNav("tags", "Tags"))));
    }

    public Optional<HubSectionNav> findSection(NavigationHub hub, String slug, LoggedUser user) {
        return groups(hub, user).stream()
                                .flatMap(g -> g.sections().stream())
                                .filter(s -> s.slug().equals(slug))
                                .findFirst();
    }

    public List<HubNavGroup> groups(NavigationHub hub, LoggedUser user) {
        return switch (hub) {
            case WRITING -> writingGroups();
            case READING -> readingGroups();
            case MANAGE -> manageGroups(user);
            case ACCOUNT -> accountGroups();
            case REVIEW -> editorGroups();
            case ADMINISTRATION -> administrationGroups();
        };
    }

    public boolean isSingleSectionHub(NavigationHub hub, LoggedUser user) {
        var groups = groups(hub, user);
        return groups.size() == 1 && groups.get(0).sections().size() == 1;
    }

    private List<HubNavGroup> manageGroups(LoggedUser user) {
        var contentSections = new ArrayList<HubSectionNav>();
        if (user != null && user.isEditor()) {
            contentSections.add(new HubSectionNav("blogs", "Blogs", "manage.nav.blogs"));
        }
        contentSections.add(new HubSectionNav("pages", "Páginas personalizadas", "manage.nav.customPages"));
        return List.of(new HubNavGroup("Visão geral",
                                       "manage.nav.overview",
                                       List.of(new HubSectionNav("dashboard", "Painel", "manage.nav.dashboard"))),
                       new HubNavGroup("Conteúdo", "manage.nav.content", List.copyOf(contentSections)),
                       new HubNavGroup("Comunidade",
                                       "manage.nav.community",
                                       List.of(new HubSectionNav("comments", "Comentários", "manage.nav.comments"))));
    }

    private List<HubNavGroup> readingGroups() {
        return List.of(new HubNavGroup("Reading",
                                       List.of(new HubSectionNav("saved", "Para ler", "reading.nav.saved"),
                                               new HubSectionNav("highlights", "Highlights"),
                                               new HubSectionNav("notes", "Notes"))));
    }

    public HubSectionNav requireSection(NavigationHub hub, String slug, LoggedUser user) {
        return findSection(hub, slug, user).orElseThrow(() -> new NotFoundException("Unknown hub section: %s".formatted(slug)));
    }

    public String sectionPath(NavigationHub hub, String slug) {
        return "%s/%s".formatted(hub.path(), slug);
    }

    private List<HubNavGroup> writingGroups() {
        return List.of(new HubNavGroup("Writing",
                                       List.of(new HubSectionNav("library", "Library"),
                                               new HubSectionNav("images", "Images"),
                                               new HubSectionNav("blogs", "Blogs"),
                                               new HubSectionNav("highlights", "Destaques e respostas"))),
                       new HubNavGroup("Author", List.of(new HubSectionNav("appearance", "Appearance"))));
    }
}
