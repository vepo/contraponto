package dev.vepo.contraponto.activitypub.inbox;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.user.User;

/**
 * Resolves inbound ActivityPub {@code Like} object URIs to a published
 * {@link Post} owned by the local actor's user.
 */
@ApplicationScoped
public class ActivityPubPostResolver {

    private static final Pattern CREATE_ACTIVITY_SUFFIX = Pattern.compile("/activities/create/(\\d+)$");

    /**
     * Platform main {@code /{username}/post/{slug}} or subdomain secondary
     * {@code /{blogSlug}/post/{slug}}.
     */
    private static final Pattern TWO_SEGMENT_POST = Pattern.compile("^/([^/]+)/post/([^/]+)$");

    private static final Pattern PLATFORM_SECONDARY_POST = Pattern.compile("^/([^/]+)/([^/]+)/post/([^/]+)$");

    private static final Pattern SUBDOMAIN_MAIN_POST = Pattern.compile("^/post/([^/]+)$");

    static String normalizeObjectUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        var trimmed = uri.trim();
        var hash = trimmed.indexOf('#');
        if (hash >= 0) {
            trimmed = trimmed.substring(0, hash);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private final BlogSubdomainConfig subdomainConfig;

    private final PostRepository postRepository;

    @Inject
    public ActivityPubPostResolver(BlogSubdomainConfig subdomainConfig, PostRepository postRepository) {
        this.subdomainConfig = subdomainConfig;
        this.postRepository = postRepository;
    }

    private boolean objectUriMatches(Post post, String normalizedUri) {
        if (normalizedUri.equals(normalizeObjectUri(ActivityPubPaths.postObjectId(post, subdomainConfig)))) {
            return true;
        }
        // Legacy Creates used the platform host as object id before the Mastodon
        // same-origin fix.
        return normalizedUri.equals(normalizeObjectUri(subdomainConfig.platformUrl(PostPaths.extractUrl(post))));
    }

    private Optional<Post> resolveByCanonicalPostUrl(String normalizedUri, User owner) {
        URI uri;
        try {
            uri = URI.create(normalizedUri);
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
        var path = uri.getPath();
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        var platformSecondary = PLATFORM_SECONDARY_POST.matcher(path);
        if (platformSecondary.matches()) {
            var username = platformSecondary.group(1);
            var blogSlug = platformSecondary.group(2);
            var slug = platformSecondary.group(3);
            if (!owner.getUsername().equals(username)) {
                return Optional.empty();
            }
            return postRepository.findBlogPost(username, blogSlug, slug)
                                 .filter(Post::isPublished)
                                 .filter(post -> objectUriMatches(post, normalizedUri));
        }
        var twoSegment = TWO_SEGMENT_POST.matcher(path);
        if (twoSegment.matches()) {
            var first = twoSegment.group(1);
            var slug = twoSegment.group(2);
            if (owner.getUsername().equals(first)) {
                return postRepository.findMainBlogPost(first, slug)
                                     .filter(Post::isPublished)
                                     .filter(post -> objectUriMatches(post, normalizedUri));
            }
            return postRepository.findBlogPost(owner.getUsername(), first, slug)
                                 .filter(Post::isPublished)
                                 .filter(post -> objectUriMatches(post, normalizedUri));
        }
        var subdomainMain = SUBDOMAIN_MAIN_POST.matcher(path);
        if (subdomainMain.matches()) {
            var slug = subdomainMain.group(1);
            return postRepository.findMainBlogPost(owner.getUsername(), slug)
                                 .filter(Post::isPublished)
                                 .filter(post -> objectUriMatches(post, normalizedUri));
        }
        return Optional.empty();
    }

    private Optional<Post> resolveByCreateActivityId(String normalizedUri, User owner) {
        var matcher = CREATE_ACTIVITY_SUFFIX.matcher(normalizedUri);
        if (!matcher.find()) {
            return Optional.empty();
        }
        long postId;
        try {
            postId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
        return postRepository.findByIdWithBlog(postId)
                             .filter(Post::isPublished)
                             .filter(post -> post.getBlog() != null
                                     && post.getBlog().isActive()
                                     && post.getBlog().getOwner().getId().equals(owner.getId()))
                             .filter(post -> normalizedUri.equals(normalizeObjectUri(ActivityPubPaths.activityId(owner,
                                                                                                                 subdomainConfig,
                                                                                                                 "create",
                                                                                                                 post.getId()))));
    }

    /**
     * Finds a published post for the given ActivityPub object URI when it belongs
     * to {@code owner}.
     */
    public Optional<Post> resolvePublishedPostOwnedBy(String objectUri, User owner) {
        if (objectUri == null || objectUri.isBlank() || owner == null) {
            return Optional.empty();
        }
        var normalized = normalizeObjectUri(objectUri);
        return resolveByCreateActivityId(normalized, owner).or(() -> resolveByCanonicalPostUrl(normalized, owner));
    }
}
