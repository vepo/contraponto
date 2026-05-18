package dev.vepo.contraponto.navigation;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BreadcrumbService {

    public static final String HOME_LABEL = "Home";
    public static final String HOME_PATH = "/";
    public static final String TAGS_SEGMENT_LABEL = "Tag";

    private static String authorBlogUrl(User author) {
        return "/%s".formatted(author.getUsername());
    }

    private static BreadcrumbItem current(String label) {
        return new BreadcrumbItem(label, null);
    }

    private static BreadcrumbItem link(String label, String href) {
        return new BreadcrumbItem(label, href);
    }

    private static String pageTitle(CustomPage page) {
        var title = page.getTitle();
        return title == null || title.isBlank() ? "Page" : title;
    }

    private static String postTitle(Post post) {
        if (post.getLivePublication() != null && post.getLivePublication().getTitle() != null) {
            return post.getLivePublication().getTitle();
        }
        return post.getTitle();
    }

    private static BreadcrumbItem text(String label) {
        return new BreadcrumbItem(label, "");
    }

    private static BreadcrumbTrail trail(BreadcrumbItem... items) {
        return new BreadcrumbTrail(List.of(items));
    }

    private static BreadcrumbTrail trail(List<BreadcrumbItem> items) {
        return new BreadcrumbTrail(List.copyOf(items));
    }

    public BreadcrumbTrail accountNotifications() {
        return hubThenCurrent(NavigationHub.ACCOUNT, "Notifications");
    }

    public BreadcrumbTrail accountSettings() {
        return hubThenCurrent(NavigationHub.ACCOUNT, "Settings");
    }

    public BreadcrumbTrail accountSubscriptions() {
        return hubThenCurrent(NavigationHub.ACCOUNT, "Subscriptions");
    }

    public BreadcrumbTrail administrationUserForm(String currentLabel) {
        return trail(link(NavigationHub.ADMINISTRATION.label(), NavigationHub.ADMINISTRATION.path()),
                     link("Users", "/users"),
                     current(currentLabel));
    }

    public BreadcrumbTrail administrationUsers() {
        return hubThenCurrent(NavigationHub.ADMINISTRATION, "Users");
    }

    public BreadcrumbTrail empty() {
        return BreadcrumbTrail.EMPTY;
    }

    public BreadcrumbTrail forCustomPage(CustomPage page) {
        var blog = page.getBlog();
        var items = new ArrayList<BreadcrumbItem>();
        items.add(link(HOME_LABEL, HOME_PATH));
        if (blog == null) {
            items.add(current(pageTitle(page)));
            return trail(items);
        }
        var author = blog.getOwner();
        items.add(link(author.getName(), authorBlogUrl(author)));
        if (!blog.isMain()) {
            items.add(link(blog.getName(), BlogEndpoint.extractUrl(blog)));
        }
        items.add(current(pageTitle(page)));
        return trail(items);
    }

    public BreadcrumbTrail forMainBlog(User author) {
        return trail(link(HOME_LABEL, HOME_PATH), current(author.getName()));
    }

    public BreadcrumbTrail forPasswordRecovery() {
        return trail(link(HOME_LABEL, HOME_PATH), current("Reset your password"));
    }

    public BreadcrumbTrail forPasswordReset() {
        return trail(link(HOME_LABEL, HOME_PATH), current("Choose a new password"));
    }

    public BreadcrumbTrail forPost(Post post) {
        return forPost(post, postTitle(post));
    }

    private BreadcrumbTrail forPost(Post post, String title) {
        var blog = post.getBlog();
        var author = blog.getOwner();
        var items = new ArrayList<BreadcrumbItem>();
        items.add(link(HOME_LABEL, HOME_PATH));
        items.add(link(author.getName(), authorBlogUrl(author)));
        if (!blog.isMain()) {
            items.add(link(blog.getName(), BlogEndpoint.extractUrl(blog)));
        }
        items.add(current(title == null || title.isBlank() ? "Untitled" : title));
        return trail(items);
    }

    public BreadcrumbTrail forPost(PublishedPostView view) {
        return forPost(view.post(), TemplateExtensions.liveTitle(view));
    }

    public BreadcrumbTrail forSearch() {
        return trail(link(HOME_LABEL, HOME_PATH), current("Search"));
    }

    public BreadcrumbTrail forSecondaryBlog(User author, Blog blog) {
        return trail(link(HOME_LABEL, HOME_PATH),
                     link(author.getName(), authorBlogUrl(author)),
                     current(blog.getName()));
    }

    public BreadcrumbTrail forSerie(Serie serie) {
        var blog = serie.getBlog();
        var author = blog.getOwner();
        var items = new ArrayList<BreadcrumbItem>();
        items.add(link(HOME_LABEL, HOME_PATH));
        items.add(link(author.getName(), authorBlogUrl(author)));
        if (!blog.isMain()) {
            items.add(link(blog.getName(), BlogEndpoint.extractUrl(blog)));
        }
        items.add(current(serie.getTitle()));
        return trail(items);
    }

    public BreadcrumbTrail forTag(Tag tag) {
        return trail(link(HOME_LABEL, HOME_PATH), text(TAGS_SEGMENT_LABEL), current(tag.getName()));
    }

    public BreadcrumbTrail hub(NavigationHub hub) {
        return trail(current(hub.label()));
    }

    private BreadcrumbTrail hubThenCurrent(NavigationHub hub, String currentLabel) {
        return trail(link(hub.label(), hub.path()), current(currentLabel));
    }

    public BreadcrumbTrail manageBlogEdit(Blog blog) {
        return trail(link(NavigationHub.MANAGE.label(), NavigationHub.MANAGE.path()),
                     link("Blogs", "/blogs"),
                     current(blog.getName()));
    }

    public BreadcrumbTrail manageBlogGitSync(Blog blog) {
        return trail(link(NavigationHub.MANAGE.label(), NavigationHub.MANAGE.path()),
                     link("Blogs", "/blogs"),
                     link(blog.getName(), "/blogs/" + blog.getId() + "/edit"),
                     current("Git sync history"));
    }

    public BreadcrumbTrail manageBlogGitSyncRun(Blog blog, String runLabel) {
        return trail(link(NavigationHub.MANAGE.label(), NavigationHub.MANAGE.path()),
                     link("Blogs", "/blogs"),
                     link(blog.getName(), "/blogs/" + blog.getId() + "/edit"),
                     link("Git sync history", "/blogs/" + blog.getId() + "/git-sync"),
                     current(runLabel));
    }

    public BreadcrumbTrail manageBlogImages(Blog blog) {
        return trail(link(NavigationHub.MANAGE.label(), NavigationHub.MANAGE.path()),
                     link("Blogs", "/blogs"),
                     link(blog.getName(), "/blogs/" + blog.getId() + "/edit"),
                     current("Images"));
    }

    public BreadcrumbTrail manageBlogNew() {
        return trail(link(NavigationHub.MANAGE.label(), NavigationHub.MANAGE.path()),
                     link("Blogs", "/blogs"),
                     current("New Blog"));
    }

    public BreadcrumbTrail manageBlogs() {
        return hubThenCurrent(NavigationHub.MANAGE, "Blogs");
    }

    public BreadcrumbTrail manageComments() {
        return hubThenCurrent(NavigationHub.MANAGE, "Comments");
    }

    public BreadcrumbTrail manageCustomPageForm(String currentLabel) {
        return trail(link(NavigationHub.MANAGE.label(), NavigationHub.MANAGE.path()),
                     link("Custom Pages", "/pages"),
                     current(currentLabel));
    }

    public BreadcrumbTrail manageCustomPages() {
        return hubThenCurrent(NavigationHub.MANAGE, "Custom Pages");
    }

    public BreadcrumbTrail manageDashboard() {
        return hubThenCurrent(NavigationHub.MANAGE, "Dashboard");
    }

    public BreadcrumbTrail reviewFeaturedPosts() {
        return hubThenCurrent(NavigationHub.REVIEW, "Featured Posts");
    }

    public BreadcrumbTrail reviewTagEdit(Tag tag) {
        return trail(link(NavigationHub.REVIEW.label(), NavigationHub.REVIEW.path()),
                     link("Tags", "/tags/manage"),
                     current(tag.getName()));
    }

    public BreadcrumbTrail reviewTags() {
        return hubThenCurrent(NavigationHub.REVIEW, "Tags");
    }

    public BreadcrumbTrail writingDraft(String draftTitle) {
        String label = draftTitle == null || draftTitle.isBlank() ? "Write" : draftTitle;
        return trail(link(NavigationHub.WRITING.label(), NavigationHub.WRITING.path()), current(label));
    }

    public BreadcrumbTrail writingLibrary() {
        return hubThenCurrent(NavigationHub.WRITING, "Library");
    }

    public BreadcrumbTrail writingWrite() {
        return hubThenCurrent(NavigationHub.WRITING, "Write");
    }
}
