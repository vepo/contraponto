package dev.vepo.contraponto.components;

import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MenuNavigationService {

    private final BlogPublicUrlService blogPublicUrlService;

    @Inject
    public MenuNavigationService(BlogPublicUrlService blogPublicUrlService) {
        this.blogPublicUrlService = blogPublicUrlService;
    }

    public MenuNavigation build(LoggedUser loggedUser) {
        var user = loggedUser.getUser();
        var workspaceUsesHtmx = !blogPublicUrlService.usesPlatformForWorkspaceLinks();
        return new MenuNavigation(blogPublicUrlService.mainBlogMenuUrl(user),
                                  blogPublicUrlService.mainBlogMenuUsesHtmx(user),
                                  blogPublicUrlService.workspaceMenuUrl("/write"),
                                  workspaceUsesHtmx,
                                  blogPublicUrlService.workspaceMenuUrl("/writing"),
                                  workspaceUsesHtmx,
                                  blogPublicUrlService.workspaceMenuUrl("/reading"),
                                  workspaceUsesHtmx,
                                  blogPublicUrlService.workspaceMenuUrl("/manage"),
                                  workspaceUsesHtmx,
                                  blogPublicUrlService.workspaceMenuUrl("/account"),
                                  workspaceUsesHtmx,
                                  blogPublicUrlService.workspaceMenuUrl("/editor"),
                                  workspaceUsesHtmx,
                                  blogPublicUrlService.workspaceMenuUrl("/administration"),
                                  workspaceUsesHtmx);
    }
}
