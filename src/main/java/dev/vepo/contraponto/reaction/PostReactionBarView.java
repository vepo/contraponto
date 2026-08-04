package dev.vepo.contraponto.reaction;

import java.util.List;

public record PostReactionBarView(long postId, List<ReactionCountView> reactions, boolean authenticated, boolean canReact) {

    public PostReactionBarView withAdditionalLikeCount(long additional) {
        if (additional <= 0) {
            return this;
        }
        List<ReactionCountView> updated = reactions.stream()
                                                   .map(r -> r.type() == ReactionType.LIKE ? r.withAdditionalCount(additional) : r)
                                                   .toList();
        return new PostReactionBarView(postId, updated, authenticated, canReact);
    }
}
