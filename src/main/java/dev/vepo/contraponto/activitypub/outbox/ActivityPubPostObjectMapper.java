package dev.vepo.contraponto.activitypub.outbox;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.image.ImageDisplayWidth;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagPaths;

/**
 * Maps published posts to ActivityStreams Create / Delete / Note / Article
 * documents for outbox and outbound delivery.
 */
@ApplicationScoped
public class ActivityPubPostObjectMapper {

    private static final List<Object> CONTEXT = List.of("https://www.w3.org/ns/activitystreams",
                                                        Map.of("Hashtag", "https://www.w3.org/ns/activitystreams#Hashtag"));

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static String coverMediaType(Image cover) {
        var contentType = cover.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "image/jpeg";
        }
        return contentType;
    }

    private static String hashtagName(Tag tag) {
        var slug = tag.getSlug();
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return "#%s".formatted(slug.trim());
    }

    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubPostObjectMapper(BlogSubdomainConfig subdomainConfig) {
        this.subdomainConfig = subdomainConfig;
    }

    private String absoluteCoverUrl(String path) {
        var normalized = path.startsWith("/") ? path : "/%s".formatted(path);
        if (normalized.startsWith("/api/images/")) {
            normalized = "%s?w=%d".formatted(normalized, ImageDisplayWidth.CARD.pixels());
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        return subdomainConfig.platformUrl(normalized);
    }

    private String buildContent(Post post, String canonicalUrl) {
        var title = post.getTitle() == null ? "" : post.getTitle().trim();
        var blog = post.getBlog();
        String body;
        if (blog != null && !blog.isMain()) {
            var blogName = blog.getName() == null ? "" : blog.getName().trim();
            body = "<p><strong>%s</strong></p><p>%s</p><p><a href=\"%s\">%s</a></p>".formatted(title,
                                                                                               blogName,
                                                                                               canonicalUrl,
                                                                                               canonicalUrl);
        } else {
            body = "<p><strong>%s</strong></p><p><a href=\"%s\">%s</a></p>".formatted(title, canonicalUrl, canonicalUrl);
        }
        var hashtagsHtml = buildHashtagsContentHtml(resolveTags(post));
        if (hashtagsHtml.isEmpty()) {
            return body;
        }
        return "%s%s".formatted(body, hashtagsHtml);
    }

    private Optional<Map<String, Object>> buildCoverAttachment(Post post) {
        return resolveCover(post).flatMap(cover -> {
            var path = cover.getUrl();
            if (path == null || path.isBlank()) {
                return Optional.empty();
            }
            var mediaUrl = absoluteCoverUrl(path);
            var attachment = new LinkedHashMap<String, Object>();
            attachment.put("type", "Image");
            attachment.put("mediaType", coverMediaType(cover));
            attachment.put("url", mediaUrl);
            var alt = cover.getAltText();
            if (alt != null && !alt.isBlank()) {
                attachment.put("name", alt.trim());
            } else if (post.getTitle() != null && !post.getTitle().isBlank()) {
                attachment.put("name", post.getTitle().trim());
            }
            return Optional.of(attachment);
        });
    }

    private String buildHashtagsContentHtml(List<Tag> tags) {
        if (tags.isEmpty()) {
            return "";
        }
        var links = new ArrayList<String>();
        for (var tag : tags) {
            var name = hashtagName(tag);
            if (name == null) {
                continue;
            }
            var href = subdomainConfig.platformUrl(TagPaths.url(tag));
            links.add("<a href=\"%s\" class=\"mention hashtag\" rel=\"tag\">%s</a>".formatted(href, name));
        }
        if (links.isEmpty()) {
            return "";
        }
        return "<p>%s</p>".formatted(String.join(" ", links));
    }

    private List<Map<String, Object>> buildHashtagTags(List<Tag> tags) {
        var result = new ArrayList<Map<String, Object>>();
        for (var tag : tags) {
            var name = hashtagName(tag);
            if (name == null) {
                continue;
            }
            var entry = new LinkedHashMap<String, Object>();
            entry.put("type", "Hashtag");
            entry.put("name", name);
            entry.put("href", subdomainConfig.platformUrl(TagPaths.url(tag)));
            result.add(entry);
        }
        return result;
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

    private Optional<Image> resolveCover(Post post) {
        var live = post.getLivePublication();
        if (live != null && live.getCover() != null) {
            return Optional.of(live.getCover());
        }
        if (post.getCover() != null) {
            return Optional.of(post.getCover());
        }
        return Optional.empty();
    }

    private List<Tag> resolveTags(Post post) {
        var live = post.getLivePublication();
        if (live != null && live.getTags() != null && !live.getTags().isEmpty()) {
            return List.copyOf(live.getTags());
        }
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            return List.copyOf(post.getTags());
        }
        return List.of();
    }

    /**
     * Builds a public {@code Create} activity wrapping the post object.
     */
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

    /**
     * Builds a public {@code Delete} activity for an unpublished post object.
     */
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

    /**
     * Builds the ActivityStreams {@code Note} / {@code Article} for a published
     * post: title + link content, optional hashtags, optional cover
     * {@code attachment}.
     */
    public Map<String, Object> toObject(Post post) {
        var objectId = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var canonicalUrl = objectId;
        var tags = resolveTags(post);
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
        buildCoverAttachment(post).ifPresent(attachment -> document.put("attachment", List.of(attachment)));
        var hashtagTags = buildHashtagTags(tags);
        if (!hashtagTags.isEmpty()) {
            document.put("tag", hashtagTags);
        }
        document.put("to", List.of("https://www.w3.org/ns/activitystreams#Public"));
        document.put("cc", List.of(ActivityPubPaths.followers(post.getBlog().getOwner(), subdomainConfig)));
        return document;
    }
}
