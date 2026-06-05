package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.git.GitSyncRun;
import dev.vepo.contraponto.blog.BlogBannerService;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.notification.Notification;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.blog.BlogDescriptionRenderer;
import dev.vepo.contraponto.content.render.PostContentRenderer;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
import dev.vepo.contraponto.rss.RssFeedPaths;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagPageEndpoint;
import java.util.List;

import io.quarkus.qute.TemplateExtension;
import jakarta.enterprise.inject.spi.CDI;

@TemplateExtension
public class TemplateExtensions {

    private static final String DEFAULT_POST_TITLE = "a post";
    private static final String DEFAULT_ACTOR_NAME = "Someone";

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, String> HTML_ENTITIES = Map.of("&", "&amp;",
                                                                    "<", "&lt;",
                                                                    ">", "&gt;",
                                                                    "\"", "&quot;",
                                                                    "'", "&#39;");

    private static final Pattern HTML_SPECIAL_CHARS = Pattern.compile(HTML_ENTITIES.keySet()
                                                                                   .stream()
                                                                                   .map(Pattern::quote)
                                                                                   .collect(Collectors.joining("|")));

    @TemplateExtension
    public static String avatarUrl(LoggedUser user) {
        return AvatarUrls.avatarUrl(user);
    }

    @TemplateExtension
    public static String avatarUrl(User user) {
        return AvatarUrls.avatarUrl(user);
    }

    @TemplateExtension
    public static String bannerUrl(Blog blog) {
        if (blog == null) {
            return null;
        }
        var service = CDI.current().select(BlogBannerService.class);
        if (!service.isResolvable()) {
            return null;
        }
        return service.get().effectiveBannerUrl(blog);
    }

    @TemplateExtension
    public static String blogGridLoadMorePath(Blog blog) {
        if (blog.isMain()) {
            return blogGridLoadMorePath(blog.getOwner().getUsername());
        }
        return "/%s/%s/components/grid".formatted(blog.getOwner().getUsername(), blog.getSlug());
    }

    @TemplateExtension
    public static String blogGridLoadMorePath(String username) {
        return "/%s/components/grid".formatted(username);
    }

    @TemplateExtension
    public static String coverUrl(Post post) {
        if (post == null) {
            return null;
        }
        PostPublication live = post.getLivePublication();
        if (live != null && live.getCover() != null) {
            return live.getCover().getUrl();
        }
        if (post.getCover() != null) {
            return post.getCover().getUrl();
        }
        return null;
    }

    @TemplateExtension
    public static String escapeHtml(String value) {
        if (Objects.nonNull(value)) {
            return HTML_SPECIAL_CHARS.matcher(value)
                                     .replaceAll(m -> HTML_ENTITIES.get(m.group()));
        } else {
            return "null";
        }
    }

    @TemplateExtension
    public static String firstName(LoggedUser user) {
        if (user == null) {
            return "";
        }
        return user.getFirstName();
    }

    @TemplateExtension
    public static String formatDate(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        return date.format(formatter);
    }

    @TemplateExtension
    public static String formatReadingDuration(long seconds) {
        if (seconds <= 0) {
            return "< 1 min";
        }
        if (seconds < 60) {
            return "< 1 min";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return "%s min".formatted(minutes);
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return "%sh".formatted(hours);
        }
        return "%sh %sm".formatted(hours, remainingMinutes);
    }

    @TemplateExtension
    public static String formatReadingTimeTotal(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 min";
        }
        long minutes = totalSeconds / 60;
        if (minutes < 60) {
            return "%s min".formatted(minutes);
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return "%sh".formatted(hours);
        }
        return "%sh %sm".formatted(hours, remainingMinutes);
    }

    @TemplateExtension
    public static String linkUrl(Notification notification) {
        if ((notification.getType() == NotificationType.GIT_SYNC_SUCCEEDED
                || notification.getType() == NotificationType.GIT_SYNC_FAILED)
                && notification.getGitSyncRun() != null) {
            GitSyncRun run = notification.getGitSyncRun();
            return "/blogs/%s/git-sync/%s".formatted(run.getBlog().getId(), run.getId());
        }
        if ((notification.getType() == NotificationType.NEW_POST
                || notification.getType() == NotificationType.NEW_COMMENT
                || notification.getType() == NotificationType.COMMON_HIGHLIGHT_PROPOSAL
                || notification.getType() == NotificationType.PUBLIC_HIGHLIGHT_NOTE
                || notification.getType() == NotificationType.POST_RESPONSE)
                && notification.getPost() != null) {
            String url = PostEndpoint.extractUrl(notification.getPost());
            if (notification.getType() == NotificationType.NEW_COMMENT) {
                return "%s#comments".formatted(url);
            }
            if (notification.getType() == NotificationType.COMMON_HIGHLIGHT_PROPOSAL
                    || notification.getType() == NotificationType.PUBLIC_HIGHLIGHT_NOTE
                    || notification.getType() == NotificationType.POST_RESPONSE) {
                return "/writing/highlights";
            }
            return url;
        }
        return BlogEndpoint.extractUrl(notification.getBlog());
    }

    @TemplateExtension
    public static String liveContent(Post post) {
        PostPublication live = liveOf(post);
        return live != null && live.getContent() != null ? live.getContent() : post.getContent();
    }

    @TemplateExtension
    public static String liveDescription(Post post) {
        if (post == null) {
            return "";
        }
        PostPublication live = liveOf(post);
        if (live != null && live.getDescription() != null && !live.getDescription().isBlank()) {
            return live.getDescription();
        }
        return post.getDescription() != null ? post.getDescription() : "";
    }

    private static PostPublication liveOf(Post post) {
        return post != null ? post.getLivePublication() : null;
    }

    private static PostPublication liveOf(PublishedPostView view) {
        if (view.live() != null) {
            return view.live();
        }
        return liveOf(view.post());
    }

    @TemplateExtension
    public static LocalDateTime livePublishedAt(PublishedPostView view) {
        PostPublication live = liveOf(view);
        if (live != null) {
            return live.getPublishedAt();
        }
        return view.post().getPublishedAt();
    }

    @TemplateExtension
    public static List<Tag> liveTags(PublishedPostView view) {
        PostPublication live = liveOf(view);
        if (live != null && !live.getTags().isEmpty()) {
            return live.getTags();
        }
        return view.post().getTags();
    }

    @TemplateExtension
    public static String liveTitle(Post post) {
        PostPublication live = liveOf(post);
        return live != null && live.getTitle() != null ? live.getTitle() : post.getTitle();
    }

    @TemplateExtension
    public static String liveTitle(PublishedPostView view) {
        PostPublication live = liveOf(view);
        Post post = view.post();
        return live != null && live.getTitle() != null ? live.getTitle() : post.getTitle();
    }

    @TemplateExtension
    public static int liveVersion(PublishedPostView view) {
        PostPublication live = liveOf(view);
        return live != null ? live.getVersion() : 0;
    }

    @TemplateExtension
    public static String message(Notification notification) {
        String blogName = notification.getBlog().getName();
        return switch (notification.getType()) {
            case NEW_POST -> {
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : DEFAULT_POST_TITLE;
                }
                yield "%s published %s".formatted(blogName, title);
            }
            case NEW_FOLLOW -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                yield "%s started following %s".formatted(actor, blogName);
            }
            case NEW_SUBSCRIBE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                yield "%s subscribed by email to %s".formatted(actor, blogName);
            }
            case NEW_COMMENT -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : DEFAULT_POST_TITLE;
                }
                yield "%s commented on %s".formatted(actor, title);
            }
            case COMMON_HIGHLIGHT_PROPOSAL -> {
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                yield "Readers often highlighted a passage on %s".formatted(title);
            }
            case PUBLIC_HIGHLIGHT_NOTE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                yield "%s submitted a public highlight note on %s".formatted(actor, title);
            }
            case POST_RESPONSE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                yield "%s published a response to %s".formatted(actor, title);
            }
            case GIT_SYNC_SUCCEEDED -> "Git sync succeeded for %s".formatted(blogName);
            case GIT_SYNC_FAILED -> "Git sync failed for %s".formatted(blogName);
        };
    }

    private static PostContentRenderer postContentRenderer() {
        return CDI.current().select(PostContentRenderer.class).get();
    }

    @TemplateExtension
    public static String readTime(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "0 min read";
        }

        String[] words = content.trim().split("\\s+");
        int wordCount = words.length;
        int wordsPerMinute = 200; // average reading speed
        int minutes = (int) Math.ceil((double) wordCount / wordsPerMinute);

        if (minutes < 1) {
            return "< 1 min read";
        }
        return "%s min read".formatted(minutes);
    }

    @TemplateExtension
    public static String render(Post post) {
        PostPublication live = post != null ? post.getLivePublication() : null;
        if (live != null) {
            return render(live);
        }
        if (post == null || post.getContent() == null || post.getContent().trim().isEmpty()) {
            return "";
        }
        return postContentRenderer().render(post.getContent(), post.getFormat());
    }

    @TemplateExtension
    public static String render(PostPublication publication) {
        if (publication == null || publication.getContent() == null || publication.getContent().trim().isEmpty()) {
            return "";
        }
        return postContentRenderer().render(publication.getContent(), publication.getFormat());
    }

    @TemplateExtension
    public static String render(PublishedPostView view) {
        if (view.live() != null) {
            return render(view.live());
        }
        return render(view.post());
    }

    @TemplateExtension
    public static String renderedDescription(Blog blog) {
        if (blog == null || blog.getDescription() == null || blog.getDescription().isBlank()) {
            return "";
        }
        return renderMarkdownDescription(blog.getDescription());
    }

    @TemplateExtension
    public static String renderedDescription(Post post) {
        String description = liveDescription(post);
        if (description.isBlank()) {
            return "";
        }
        return renderMarkdownDescription(description);
    }

    private static String renderMarkdownDescription(String description) {
        var renderer = CDI.current().select(BlogDescriptionRenderer.class);
        if (!renderer.isResolvable()) {
            return description;
        }
        return renderer.get().render(description);
    }

    @TemplateExtension
    public static String rssFeedUrl(Blog blog) {
        return blog == null ? null : RssFeedPaths.blogFeed(blog);
    }

    @TemplateExtension
    public static String rssFeedUrl(Serie serie) {
        return serie == null ? null : RssFeedPaths.serieFeed(serie);
    }

    @TemplateExtension
    public static String rssFeedUrl(Tag tag) {
        return tag == null ? null : RssFeedPaths.tagFeed(tag);
    }

    @TemplateExtension
    public static boolean showUpdated(PublishedPostView view) {
        PostPublication live = liveOf(view);
        return live != null && live.getVersion() > 1;
    }

    @TemplateExtension
    public static String tagGridLoadMorePath(String tagSlug) {
        return "/tags/%s/components/grid".formatted(tagSlug);
    }

    @TemplateExtension
    public static String url(Blog blog) {
        return BlogEndpoint.extractUrl(blog);
    }

    @TemplateExtension
    public static String url(Post post) {
        return PostEndpoint.extractUrl(post);
    }

    @TemplateExtension
    public static String url(Serie serie) {
        return SeriePageEndpoint.extractUrl(serie);
    }

    @TemplateExtension
    public static String url(Tag tag) {
        return TagPageEndpoint.url(tag);
    }

    private TemplateExtensions() {
        /* This utility class should not be instantiated */
    }
}