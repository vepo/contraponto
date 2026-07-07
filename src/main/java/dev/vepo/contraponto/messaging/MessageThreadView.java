package dev.vepo.contraponto.messaging;

import java.util.List;

import dev.vepo.contraponto.user.User;

public record MessageThreadView(MessageThread thread,
                                List<ThreadMessage> messages,
                                User otherParticipant,
                                boolean showBlockedBanner) {

    public boolean canReply() {
        return thread.getStatus().acceptsReplies() && !showBlockedBanner;
    }
}
