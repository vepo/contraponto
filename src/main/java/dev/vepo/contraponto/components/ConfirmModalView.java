package dev.vepo.contraponto.components;

import io.quarkus.qute.RawString;

record ConfirmModalView(String httpMethod,
                        String actionUrl,
                        String targetSelector,
                        String swap,
                        String titleKey,
                        String titleDefault,
                        String messageKey,
                        String messageDefault,
                        String confirmKey,
                        String confirmDefault,
                        RawString extraFormFields) {

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
                                    action.confirmDefault(),
                                    null);
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
                                    action.confirmDefault(),
                                    null);
    }

    static ConfirmModalView forMessageBlock(long blockedUserId, String returnUrl) {
        var action = ConfirmModalAction.MESSAGE_BLOCK;
        String fields = "<input type=\"hidden\" name=\"blockedUserId\" value=\"%s\"/><input type=\"hidden\" name=\"returnUrl\" value=\"%s\"/>"
                                                                                                                                              .formatted(blockedUserId,
                                                                                                                                                         returnUrl == null ? ""
                                                                                                                                                                           : returnUrl);
        return new ConfirmModalView(action.httpMethod(),
                                    action.actionUrl(blockedUserId),
                                    "main",
                                    "outerHTML",
                                    action.titleKey(),
                                    action.titleDefault(),
                                    action.messageKey(),
                                    action.messageDefault(),
                                    action.confirmKey(),
                                    action.confirmDefault(),
                                    new RawString(fields));
    }
}
