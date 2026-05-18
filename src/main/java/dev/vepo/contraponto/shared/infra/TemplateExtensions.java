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
import dev.vepo.contraponto.image.RenderedHtmlEnricher;
import dev.vepo.contraponto.renderer.Renderer;
import dev.vepo.contraponto.shared.security.HtmlSanitizer;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
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
        return enrichRendered(Renderer.get(post.getFormat()).render(post.getContent()));
    }

    @TemplateExtension
    public static String render(PostPublication publication) {
        if (publication == null || publication.getContent() == null || publication.getContent().trim().isEmpty()) {
            return "";
        }
        return enrichRendered(Renderer.get(publication.getFormat()).render(publication.getContent()));
    }

    private static String enrichRendered(String html) {
        if (html == null || html.isBlank()) {
            return html == null ? "" : html;
        }
        var sanitizer = CDI.current().select(HtmlSanitizer.class);
        if (sanitizer.isResolvable()) {
            html = sanitizer.get().sanitizePostHtml(html);
        }
        var enricher = CDI.current().select(RenderedHtmlEnricher.class);
        if (!enricher.isResolvable()) {
            return html;
        }
        return enricher.get().enrichHtml(html);
    }

    @TemplateExtension
    public static String render(PublishedPostView view) {
        if (view.live() != null) {
            return render(view.live());
        }
        return render(view.post());
    }

    @TemplateExtension
    public static boolean showUpdated(PublishedPostView view) {
        PostPublication live = liveOf(view);
        return live != null && live.getVersion() > 1;
    }

    @TemplateExtension
    public static int liveVersion(PublishedPostView view) {
        PostPublication live = liveOf(view);
        return live != null ? live.getVersion() : 0;
    }

    @TemplateExtension
    public static String blogGridLoadMorePath(String username) {
        return "/%s/components/grid".formatted(username);
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

    @TemplateExtension
    public static String message(Notification notification) {
        String blogName = notification.getBlog().getName();
        return switch (notification.getType()) {
            case NEW_POST -> {
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : DEFAULT_POST_TITLE;
                }
                yield blogName + " published " + title;
            }
            case NEW_FOLLOW -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                yield actor + " started following " + blogName;
            }
            case NEW_SUBSCRIBE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                yield actor + " subscribed by email to " + blogName;
            }
            case NEW_COMMENT -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : DEFAULT_POST_TITLE;
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