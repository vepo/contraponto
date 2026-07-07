package dev.vepo.contraponto.messaging;

public enum MessageThreadStatus {
    OPEN,
    CLOSED,
    FROZEN;

    public boolean acceptsReplies() {
        return this == OPEN;
    }
}
