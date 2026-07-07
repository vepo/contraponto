package dev.vepo.contraponto.components;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

record ConfirmModalView(String httpMethod,
                        String actionUrl,
                        String targetSelector,
                        String swap,
                        String titleKey,
                        String titleDefault,
                        String messageKey,
                        String messageDefault,
                        String confirmKey,
                        String confirmDefault) {

    static ConfirmModalView forPost(ConfirmModalAction action, long postId) {
        return new ConfirmModalView(action.httpMethod(),
                                    action.actionUrl(postId),
                                    action.targetSelector(postId),
                                    "outerHTML delete",
                                    action.titleKey(),
                                    action.titleDefault(),
                                    action.messageKey(),
                                    action.messageDefault(),
                                    action.confirmKey(),
                                    action.confirmDefault());
    }

    static ConfirmModalView forMessageThread(ConfirmModalAction action, long threadId) {
        return new ConfirmModalView(action.httpMethod(),
                                    action.actionUrl(threadId),
                                    "main",
                                    "outerHTML",
                                    action.titleKey(),
                                    action.titleDefault(),
                                    action.messageKey(),
                                    action.messageDefault(),
                                    action.confirmKey(),
                                    action.confirmDefault());
    }

    static ConfirmModalView forMessageBlock(long blockedUserId, String returnUrl) {
        var action = ConfirmModalAction.MESSAGE_BLOCK;
        String actionUrl = action.actionUrl(blockedUserId);
        if (returnUrl != null && !returnUrl.isBlank()) {
            actionUrl = "%s?returnUrl=%s".formatted(actionUrl,
                                                    URLEncoder.encode(returnUrl, StandardCharsets.UTF_8));
        }
        return new ConfirmModalView(action.httpMethod(),
                                    actionUrl,
                                    "main",
                                    "outerHTML",
                                    action.titleKey(),
                                    action.titleDefault(),
                                    action.messageKey(),
                                    action.messageDefault(),
                                    action.confirmKey(),
                                    action.confirmDefault());
    }
}
