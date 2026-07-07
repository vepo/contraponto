package dev.vepo.contraponto.messaging;

public final class MessageThreadPaths {

    public static String blocked() {
        return "/account/messages/blocked";
    }

    public static String compose() {
        return "/account/messages/compose";
    }

    public static String compose(String toUsername) {
        if (toUsername == null || toUsername.isBlank()) {
            return compose();
        }
        return "%s?to=%s".formatted(compose(), toUsername);
    }

    public static String mailbox() {
        return "/account/mailbox";
    }

    public static String thread(long threadId) {
        return "/account/messages/%s".formatted(threadId);
    }

    private MessageThreadPaths() {
        throw new UnsupportedOperationException("Utility class");
    }
}
