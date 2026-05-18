package dev.vepo.contraponto.navigation;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NavigationHubLinks {

    public List<HubSection> accountSections() {
        return List.of(new HubSection("Activity",
                                      List.of(new HubLink("Notifications",
                                                          "New posts from blogs you follow.",
                                                          "/notifications"),
                                              new HubLink("Subscriptions",
                                                          "Blogs you follow or subscribe to by email.",
                                                          "/subscriptions"))),
                       new HubSection("Profile",
                                      List.of(new HubLink("Settings",
                                                          "Profile, email, and password.",
                                                          "/profile"))));
    }

    public List<HubSection> administrationSections() {
        return List.of(new HubSection("Platform",
                                      List.of(new HubLink("Users",
                                                          "Create and manage user accounts.",
                                                          "/users"))));
    }

    public List<HubSection> editorSections() {
        return List.of(new HubSection("Editorial",
                                      List.of(new HubLink("Featured Posts",
                                                          "Curate posts for the home page.",
                                                          "/review"),
                                              new HubLink("Tags",
                                                          "Edit tag names and descriptions.",
                                                          "/tags/manage"))));
    }

    public List<HubSection> manageSections() {
        return List.of(new HubSection("Overview",
                                      List.of(new HubLink("Dashboard",
                                                          "Analytics and recent activity.",
                                                          "/dashboard"))),
                       new HubSection("Content",
                                      List.of(new HubLink("Blogs",
                                                          "Manage your blogs and settings.",
                                                          "/blogs"),
                                              new HubLink("Custom Pages",
                                                          "Static pages for your site.",
                                                          "/pages"))),
                       new HubSection("Community",
                                      List.of(new HubLink("Comments",
                                                          "Moderate comments on your posts.",
                                                          "/comments"))));
    }

    public List<HubSection> writingSections() {
        return List.of(new HubSection("Writing",
                                      List.of(new HubLink("Write",
                                                          "Create or edit a post.",
                                                          "/write"),
                                              new HubLink("Library",
                                                          "Drafts and published posts.",
                                                          "/library"))));
    }
}
