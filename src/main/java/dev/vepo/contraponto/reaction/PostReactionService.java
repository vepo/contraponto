package dev.vepo.contraponto.reaction;

import java.util.Arrays;
import java.util.Set;

import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class PostReactionService {

    private final PostReactionRepository reactionRepository;
    private final NotificationService notificationService;

    @Inject
    public PostReactionService(PostReactionRepository reactionRepository, NotificationService notificationService) {
        this.reactionRepository = reactionRepository;
        this.notificationService = notificationService;
    }

    public PostReactionBarView buildView(Post post, Long userId) {
        var counts = reactionRepository.countsByPost(post.getId());
        Set<ReactionType> activeTypes = userId == null ? Set.of() : reactionRepository.findActiveTypes(post.getId(), userId);
        boolean authenticated = userId != null;
        boolean canReact = authenticated && !post.getAuthor().getId().equals(userId);
        var reactions = Arrays.stream(ReactionType.values())
                              .map(type -> new ReactionCountView(type, counts.getOrDefault(type, 0L), activeTypes.contains(type)))
                              .toList();
        return new PostReactionBarView(post.getId(), reactions, authenticated, canReact);
    }

    @Transactional
    public PostReactionBarView toggle(Post post, User user, ReactionType type) {
        if (post.getAuthor().getId().equals(user.getId())) {
            throw new BadRequestException("The post author cannot react to their own post.");
        }
        var existing = reactionRepository.find(post.getId(), user.getId(), type);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            var reaction = new PostReaction();
            reaction.setPost(post);
            reaction.setUser(user);
            reaction.setReactionType(type);
            reactionRepository.save(reaction);
            notificationService.notifyNewReaction(post.getAuthor(), post, user);
        }
        return buildView(post, user.getId());
    }
}
