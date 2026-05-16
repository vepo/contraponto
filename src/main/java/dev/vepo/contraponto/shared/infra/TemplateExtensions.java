package dev.vepo.contraponto.shared.infra;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.notification.Notification;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.renderer.Renderer;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagPageEndpoint;
import java.util.List;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {

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
        if (Objects.nonNull(user)) {
            return "https://ui-avatars.com/api/?name=%s&background=1a8917&color=fff&bold=true&length=2".formatted(URLEncoder.encode(user.getName(),
                                                                                                                                    StandardCharsets.UTF_8));
        } else {
            return "";
        }
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
    public static String liveContent(Post post) {
        PostPublication live = liveOf(post);
        return live != null && live.getContent() != null ? live.getContent() : post.getContent();
    }

    @TemplateExtension
    public static String liveDescription(Post post) {
        PostPublication live = liveOf(post);
        return live != null && live.getDescription() != null ? live.getDescription() : post.getDescription();
    }

    private static PostPublication liveOf(Post post) {
        return post != null ? post.getLivePublication() : null;
    }

    @TemplateExtension
    public static LocalDateTime livePublishedAt(Post post) {
        PostPublication live = liveOf(post);
        if (live != null) {
            return live.getPublishedAt();
        }
        return post.getPublishedAt();
    }

    @TemplateExtension
    public static LocalDateTime livePublishedAt(PublishedPostView view) {
        return livePublishedAt(view.post());
    }

    @TemplateExtension
    public static List<Tag> liveTags(Post post) {
        PostPublication live = liveOf(post);
        if (live != null && !live.getTags().isEmpty()) {
            return live.getTags();
        }
        return post.getTags();
    }

    @TemplateExtension
    public static List<Tag> liveTags(PublishedPostView view) {
        return liveTags(view.post());
    }

    @TemplateExtension
    public static String liveTitle(Post post) {
        PostPublication live = liveOf(post);
        return live != null && live.getTitle() != null ? live.getTitle() : post.getTitle();
    }

    @TemplateExtension
    public static String liveTitle(PublishedPostView view) {
        return liveTitle(view.post());
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
        return minutes + " min read";
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
        return Renderer.get(post.getFormat()).render(post.getContent());
    }

    @TemplateExtension
    public static String render(PostPublication publication) {
        if (publication == null || publication.getContent() == null || publication.getContent().trim().isEmpty()) {
            return "";
        }
        return Renderer.get(publication.getFormat()).render(publication.getContent());
    }

    @TemplateExtension
    public static String render(PublishedPostView view) {
        if (view.live() != null) {
            return render(view.live());
        }
        return render(view.post());
    }

    @TemplateExtension
    public static boolean showUpdated(Post post) {
        PostPublication live = liveOf(post);
        return live != null && live.getVersion() > 1;
    }

    @TemplateExtension
    public static boolean showUpdated(PublishedPostView view) {
        return showUpdated(view.post());
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

    @TemplateExtension
    public static String message(Notification notification) {
        String blogName = notification.getBlog().getName();
        return switch (notification.getType()) {
            case NEW_POST -> {
                String title = notification.getPost() != null ? notification.getPost().getTitle() : "a post";
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : "a post";
                }
                yield blogName + " published " + title;
            }
            case NEW_FOLLOW -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : "Someone";
                yield actor + " started following " + blogName;
            }
            case NEW_SUBSCRIBE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : "Someone";
                yield actor + " subscribed by email to " + blogName;
            }
            case NEW_COMMENT -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : "Someone";
                String title = notification.getPost() != null ? notification.getPost().getTitle() : "a post";
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : "a post";
                }
                yield actor + " commented on " + title;
            }
        };
    }

    @TemplateExtension
    public static String linkUrl(Notification notification) {
        if ((notification.getType() == NotificationType.NEW_POST
                || notification.getType() == NotificationType.NEW_COMMENT)
                && notification.getPost() != null) {
            String url = PostEndpoint.extractUrl(notification.getPost());
            if (notification.getType() == NotificationType.NEW_COMMENT) {
                return url + "#comments";
            }
            return url;
        }
        return BlogEndpoint.extractUrl(notification.getBlog());
    }

    private TemplateExtensions() {
        /* This utility class should not be instantiated */
    }
}