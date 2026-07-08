package dev.vepo.contraponto.activitypub.outbox;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;

@ApplicationScoped
public class ActivityPubPostObjectMapper {

    private static final List<String> CONTEXT = List.of("https://www.w3.org/ns/activitystreams");

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubPostObjectMapper(BlogSubdomainConfig subdomainConfig) {
        this.subdomainConfig = subdomainConfig;
    }

    private String buildContent(Post post, String canonicalUrl) {
        var title = post.getTitle() == null ? "" : post.getTitle().trim();
        var blog = post.getBlog();
        if (blog != null && !blog.isMain()) {
            var blogName = blog.getName() == null ? "" : blog.getName().trim();
            return "<p><strong>%s</strong></p><p>%s</p><p><a href=\"%s\">%s</a></p>".formatted(title,
                                                                                               blogName,
                                                                                               canonicalUrl,
                                                                                               canonicalUrl);
        }
        return "<p><strong>%s</strong></p><p><a href=\"%s\">%s</a></p>".formatted(title, canonicalUrl, canonicalUrl);
    }

    private boolean isArticle(Post post) {
        var content = post.getContent();
        return content != null && content.length() > 500;
    }

    private String publishedInstant(Post post) {
        if (post.getPublishedAt() == null) {
            return null;
        }
        return post.getPublishedAt().atZone(ZoneOffset.UTC).format(ISO_INSTANT);
    }

    public Map<String, Object> toCreateActivity(Post post) {
        var object = toObject(post);
        var activity = new LinkedHashMap<String, Object>();
        activity.put("@context", CONTEXT);
        activity.put("id", ActivityPubPaths.activityId(post.getBlog().getOwner(), subdomainConfig, "create", post.getId()));
        activity.put("type", "Create");
        activity.put("actor", ActivityPubPaths.actorId(post.getBlog().getOwner(), subdomainConfig));
        var published = publishedInstant(post);
        if (published != null) {
            activity.put("published", published);
        }
        activity.put("to", object.get("to"));
        activity.put("cc", object.get("cc"));
        activity.put("object", object);
        return activity;
    }

    public Map<String, Object> toDeleteActivity(Post post) {
        var activity = new LinkedHashMap<String, Object>();
        activity.put("@context", CONTEXT);
        activity.put("id", ActivityPubPaths.activityId(post.getBlog().getOwner(), subdomainConfig, "delete", post.getId()));
        activity.put("type", "Delete");
        activity.put("actor", ActivityPubPaths.actorId(post.getBlog().getOwner(), subdomainConfig));
        activity.put("to", List.of("https://www.w3.org/ns/activitystreams#Public"));
        activity.put("object", ActivityPubPaths.postObjectId(post, subdomainConfig));
        return activity;
    }

    public Map<String, Object> toObject(Post post) {
        var objectId = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var canonicalUrl = objectId;
        var content = buildContent(post, canonicalUrl);
        var objectType = isArticle(post) ? "Article" : "Note";
        var document = new LinkedHashMap<String, Object>();
        document.put("@context", CONTEXT);
        document.put("id", objectId);
        document.put("type", objectType);
        document.put("name", post.getTitle());
        document.put("content", content);
        document.put("url", canonicalUrl);
        document.put("attributedTo", ActivityPubPaths.actorId(post.getBlog().getOwner(), subdomainConfig));
        var published = publishedInstant(post);
        if (published != null) {
            document.put("published", published);
        }
        if (post.getUpdatedAt() != null) {
            document.put("updated", post.getUpdatedAt().atZone(ZoneOffset.UTC).format(ISO_INSTANT));
        }
        document.put("to", List.of("https://www.w3.org/ns/activitystreams#Public"));
        document.put("cc", List.of(ActivityPubPaths.followers(post.getBlog().getOwner(), subdomainConfig)));
        return document;
    }
}
