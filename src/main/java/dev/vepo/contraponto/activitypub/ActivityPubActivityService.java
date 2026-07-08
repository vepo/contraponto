package dev.vepo.contraponto.activitypub;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubActivityService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("create", "delete");

    private final ActivityPubSettings settings;
    private final ActivityPubActorService actorService;
    private final ActivityPubPostObjectMapper postObjectMapper;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Inject
    public ActivityPubActivityService(ActivityPubSettings settings,
                                      ActivityPubActorService actorService,
                                      ActivityPubPostObjectMapper postObjectMapper,
                                      PostRepository postRepository,
                                      UserRepository userRepository) {
        this.settings = settings;
        this.actorService = actorService;
        this.postObjectMapper = postObjectMapper;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public Optional<Map<String, Object>> findActivity(String username, String activityType, long activityId) {
        if (!settings.enabled() || activityType == null || !SUPPORTED_TYPES.contains(activityType)) {
            return Optional.empty();
        }
        if (actorService.findEnabledByUsername(username).isEmpty()) {
            return Optional.empty();
        }
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        var post = postRepository.findByIdWithBlog(activityId).orElse(null);
        if (!isOwnedBlogPost(post, user)) {
            return Optional.empty();
        }
        if ("create".equals(activityType) && !post.isPublished()) {
            return Optional.empty();
        }
        return Optional.of(switch (activityType) {
            case "create" -> postObjectMapper.toCreateActivity(post);
            case "delete" -> postObjectMapper.toDeleteActivity(post);
            default -> throw new IllegalStateException("Unsupported activity type: " + activityType);
        });
    }

    private boolean isOwnedBlogPost(Post post, User user) {
        if (post == null || post.getBlog() == null || !post.getBlog().isActive()) {
            return false;
        }
        return post.getBlog().getOwner().getId().equals(user.getId());
    }
}
