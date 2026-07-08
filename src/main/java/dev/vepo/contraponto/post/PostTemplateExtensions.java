package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogDescriptionRenderer;
import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.content.render.PostContentRenderer;
import dev.vepo.contraponto.image.ImageDisplayWidth;
import dev.vepo.contraponto.tag.Tag;
import io.quarkus.qute.TemplateExtension;
import jakarta.enterprise.inject.spi.CDI;

@TemplateExtension
public class PostTemplateExtensions {

    @TemplateExtension
    public static String cardCoverUrl(Post post) {
        return sizedImageUrl(coverUrl(post), ImageDisplayWidth.CARD);
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

    private static PostContentRenderer postContentRenderer() {
        return CDI.current().select(PostContentRenderer.class).get();
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
        return postContentRenderer().render(publication);
    }

    @TemplateExtension
    public static String render(PublishedPostView view) {
        if (view.live() != null) {
            return render(view.live());
        }
        return render(view.post());
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
    public static boolean showUpdated(PublishedPostView view) {
        PostPublication live = liveOf(view);
        return live != null && live.getVersion() > 1;
    }

    private static String sizedImageUrl(String url, ImageDisplayWidth width) {
        if (url == null || url.isBlank() || width == null) {
            return url;
        }
        if (!url.startsWith("/api/images/")) {
            return url;
        }
        return "%s?w=%d".formatted(url, width.pixels());
    }

    @TemplateExtension
    public static String url(Post post) {
        return CDI.current().select(BlogPublicUrlService.class).get().relativePath(post);
    }

    private PostTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
