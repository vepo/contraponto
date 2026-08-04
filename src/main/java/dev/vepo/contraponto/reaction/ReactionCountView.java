package dev.vepo.contraponto.reaction;

public record ReactionCountView(ReactionType type, long count, boolean active) {

    public ReactionCountView withAdditionalCount(long additional) {
        return new ReactionCountView(type, count + additional, active);
    }
}
