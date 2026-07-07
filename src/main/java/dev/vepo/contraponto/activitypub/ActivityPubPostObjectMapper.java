package dev.vepo.contraponto.activitypub;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        var summary = post.getDescription() == null ? "" : post.getDescription().trim();
        return "<p><strong>%s</strong></p><p>%s</p><p><a href=\"%s\">%s</a></p>".formatted(title, summary, canonicalUrl, canonicalUrl);
    }

    private boolean isArticle(Post post) {
        var content = post.getContent();
        return content != null && content.length() > 500;
    }

    public Map<String, Object> toCreateActivity(Post post) {
        var object = toObject(post);
        var activity = new LinkedHashMap<String, Object>();
        activity.put("@context", CONTEXT);
        activity.put("id", ActivityPubPaths.activityId(post.getBlog().getOwner(), subdomainConfig, "create", post.getId()));
        activity.put("type", "Create");
        activity.put("actor", ActivityPubPaths.actorId(post.getBlog().getOwner(), subdomainConfig));
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
        document.put("summary", post.getDescription());
        document.put("content", content);
        document.put("url", canonicalUrl);
        document.put("attributedTo", ActivityPubPaths.actorId(post.getBlog().getOwner(), subdomainConfig));
        if (post.getPublishedAt() != null) {
            document.put("published", post.getPublishedAt().atZone(ZoneOffset.UTC).format(ISO_INSTANT));
        }
        if (post.getUpdatedAt() != null) {
            document.put("updated", post.getUpdatedAt().atZone(ZoneOffset.UTC).format(ISO_INSTANT));
        }
        document.put("to", List.of("https://www.w3.org/ns/activitystreams#Public"));
        document.put("cc", List.of(ActivityPubPaths.followers(post.getBlog().getOwner(), subdomainConfig)));
        return document;
    }
}
