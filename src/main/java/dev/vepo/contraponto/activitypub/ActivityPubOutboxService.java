package dev.vepo.contraponto.activitypub;

import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubOutboxService {

    private static final int OUTBOX_PAGE_SIZE = 20;

    private final BlogSubdomainConfig subdomainConfig;
    private final PostRepository postRepository;
    private final ActivityPubPostObjectMapper postObjectMapper;

    @Inject
    public ActivityPubOutboxService(BlogSubdomainConfig subdomainConfig,
                                    PostRepository postRepository,
                                    ActivityPubPostObjectMapper postObjectMapper) {
        this.subdomainConfig = subdomainConfig;
        this.postRepository = postRepository;
        this.postObjectMapper = postObjectMapper;
    }

    public Map<String, Object> buildFollowersCollection(User user, List<String> followerActorIds) {
        return Map.of("@context", "https://www.w3.org/ns/activitystreams",
                      "id", ActivityPubPaths.followers(user, subdomainConfig),
                      "type", "Collection",
                      "totalItems", followerActorIds.size(),
                      "items", followerActorIds);
    }

    public Map<String, Object> buildOutbox(User user, int page) {
        var outboxId = ActivityPubPaths.outbox(user, subdomainConfig);
        var posts = postRepository.findPublishedByAuthor(user.getId(), PageQuery.forGrid(OUTBOX_PAGE_SIZE, page));
        var items = posts.data()
                         .stream()
                         .filter(post -> post.getBlog().isMain())
                         .map(postObjectMapper::toCreateActivity)
                         .toList();
        return Map.of("@context", "https://www.w3.org/ns/activitystreams",
                      "id", outboxId,
                      "type", "OrderedCollection",
                      "totalItems", posts.total(),
                      "orderedItems", items);
    }

    public Map<String, Object> buildPostObject(Post post) {
        return postObjectMapper.toObject(post);
    }
}
