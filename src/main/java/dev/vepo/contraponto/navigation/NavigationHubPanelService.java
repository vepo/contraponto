package dev.vepo.contraponto.navigation;

import java.util.Map;

import dev.vepo.contraponto.admin.ReviewEndpoint;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.blog.BlogManageEndpoint;
import dev.vepo.contraponto.comment.CommentManageEndpoint;
import dev.vepo.contraponto.components.AccountSecurityEndpoint;
import dev.vepo.contraponto.components.AuthorAppearanceEndpoint;
import dev.vepo.contraponto.custompage.CustomPageManageEndpoint;
import dev.vepo.contraponto.dashboard.DashboardAnalyticsService;
import dev.vepo.contraponto.dashboard.DashboardEndpoint;
import dev.vepo.contraponto.dashboard.DashboardPage;
import dev.vepo.contraponto.library.LibraryEndpoint;
import dev.vepo.contraponto.notification.NotificationEndpoint;
import dev.vepo.contraponto.notification.SubscriptionEndpoint;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.tag.TagManageEndpoint;
import dev.vepo.contraponto.user.UserManageEndpoint;
import dev.vepo.contraponto.view.ViewRepository;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class NavigationHubPanelService {

    private final PostRepository postRepository;
    private final ViewRepository viewRepository;
    private final DashboardAnalyticsService analyticsService;
    private final LoggedUser loggedUser;
    private final ReviewEndpoint reviewEndpoint;
    private final TagManageEndpoint tagManageEndpoint;
    private final BlogManageEndpoint blogManageEndpoint;
    private final BlogAccess blogAccess;
    private final CustomPageManageEndpoint customPageManageEndpoint;
    private final CommentManageEndpoint commentManageEndpoint;
    private final LibraryEndpoint libraryEndpoint;
    private final NotificationEndpoint notificationEndpoint;
    private final SubscriptionEndpoint subscriptionEndpoint;
    private final AccountSecurityEndpoint accountSecurityEndpoint;
    private final AuthorAppearanceEndpoint authorAppearanceEndpoint;
    private final UserManageEndpoint userManageEndpoint;
    private final NavigationHubRegistry registry;

    @Inject
    public NavigationHubPanelService(PostRepository postRepository,
                                     ViewRepository viewRepository,
                                     DashboardAnalyticsService analyticsService,
                                     LoggedUser loggedUser,
                                     ReviewEndpoint reviewEndpoint,
                                     TagManageEndpoint tagManageEndpoint,
                                     BlogManageEndpoint blogManageEndpoint,
                                     BlogAccess blogAccess,
                                     CustomPageManageEndpoint customPageManageEndpoint,
                                     CommentManageEndpoint commentManageEndpoint,
                                     LibraryEndpoint libraryEndpoint,
                                     NotificationEndpoint notificationEndpoint,
                                     SubscriptionEndpoint subscriptionEndpoint,
                                     AccountSecurityEndpoint accountSecurityEndpoint,
                                     AuthorAppearanceEndpoint authorAppearanceEndpoint,
                                     UserManageEndpoint userManageEndpoint,
                                     NavigationHubRegistry registry) {
        this.postRepository = postRepository;
        this.viewRepository = viewRepository;
        this.analyticsService = analyticsService;
        this.loggedUser = loggedUser;
        this.reviewEndpoint = reviewEndpoint;
        this.tagManageEndpoint = tagManageEndpoint;
        this.blogManageEndpoint = blogManageEndpoint;
        this.blogAccess = blogAccess;
        this.customPageManageEndpoint = customPageManageEndpoint;
        this.commentManageEndpoint = commentManageEndpoint;
        this.libraryEndpoint = libraryEndpoint;
        this.notificationEndpoint = notificationEndpoint;
        this.subscriptionEndpoint = subscriptionEndpoint;
        this.accountSecurityEndpoint = accountSecurityEndpoint;
        this.authorAppearanceEndpoint = authorAppearanceEndpoint;
        this.userManageEndpoint = userManageEndpoint;
        this.registry = registry;
    }

    public TemplateInstance render(NavigationHub hub, String sectionSlug, int page) {
        return render(hub, sectionSlug, page, false, null);
    }

    public TemplateInstance render(NavigationHub hub,
                                   String sectionSlug,
                                   int page,
                                   boolean emailVerified,
                                   String profileError) {
        registry.requireSection(hub, sectionSlug, loggedUser);
        return switch (hub) {
            case WRITING -> renderWriting(sectionSlug, page);
            case MANAGE -> renderManage(sectionSlug, page);
            case ACCOUNT -> renderAccount(sectionSlug, page, emailVerified, profileError);
            case REVIEW -> renderReview(sectionSlug, page);
            case ADMINISTRATION -> renderAdministration(sectionSlug, page);
        };
    }

    private TemplateInstance renderWriting(String sectionSlug, int page) {
        String basePath = registry.sectionPath(NavigationHub.WRITING, sectionSlug);
        return switch (sectionSlug) {
            case "library" -> libraryEndpoint.renderHubPanel();
            case "blogs" -> blogManageEndpoint.renderAuthorHubPanel(page, basePath);
            case "appearance" -> authorAppearanceEndpoint.renderHubPanel();
            default -> throw new NotFoundException("Unknown writing section: " + sectionSlug);
        };
    }

    private TemplateInstance renderManage(String sectionSlug, int page) {
        String basePath = registry.sectionPath(NavigationHub.MANAGE, sectionSlug);
        return switch (sectionSlug) {
            case "dashboard" -> renderDashboardPanel();
            case "blogs" -> {
                if (!blogAccess.canListAll(loggedUser)) {
                    throw new NotFoundException("Unknown manage section: blogs");
                }
                yield blogManageEndpoint.renderPlatformHubPanel(page, basePath);
            }
            case "pages" -> customPageManageEndpoint.renderHubPanel(page, basePath);
            case "comments" -> commentManageEndpoint.renderHubPanel(page, basePath);
            default -> throw new NotFoundException("Unknown manage section: " + sectionSlug);
        };
    }

    private TemplateInstance renderDashboardPanel() {
        var draftsCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), false);
        var publishedCount = postRepository.countByAuthorAndPublished(loggedUser.getId(), true);
        var recentDrafts = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), false, 5);
        var recentPublished = postRepository.findRecentByAuthorAndPublished(loggedUser.getId(), true, 5);
        Map<Long, Long> viewCounts = viewRepository.getViewCountsForPosts(recentPublished.stream()
                                                                                         .map(Post::getId)
                                                                                         .toList());
        Long selectedBlogId = analyticsService.resolveDefaultBlogId();
        return DashboardEndpoint.Templates.panel(new DashboardPage(draftsCount,
                                                                   publishedCount,
                                                                   recentDrafts,
                                                                   recentPublished,
                                                                   viewCounts,
                                                                   selectedBlogId));
    }

    private TemplateInstance renderAccount(String sectionSlug,
                                           int page,
                                           boolean emailVerified,
                                           String profileError) {
        String basePath = registry.sectionPath(NavigationHub.ACCOUNT, sectionSlug);
        return switch (sectionSlug) {
            case "notifications" -> notificationEndpoint.renderHubPanel(page, basePath);
            case "subscriptions" -> subscriptionEndpoint.renderHubPanel(page, basePath);
            case "security" -> accountSecurityEndpoint.renderHubPanel(emailVerified, profileError);
            default -> throw new NotFoundException("Unknown account section: " + sectionSlug);
        };
    }

    private TemplateInstance renderReview(String sectionSlug, int page) {
        String basePath = registry.sectionPath(NavigationHub.REVIEW, sectionSlug);
        return switch (sectionSlug) {
            case "review" -> reviewEndpoint.renderHubPanel(page, basePath);
            case "tags" -> tagManageEndpoint.renderHubPanel(page, basePath);
            default -> throw new NotFoundException("Unknown editor section: " + sectionSlug);
        };
    }

    private TemplateInstance renderAdministration(String sectionSlug, int page) {
        if ("users".equals(sectionSlug)) {
            return userManageEndpoint.renderHubPanel(page, registry.sectionPath(NavigationHub.ADMINISTRATION, sectionSlug));
        }
        throw new NotFoundException("Unknown administration section: " + sectionSlug);
    }
}
