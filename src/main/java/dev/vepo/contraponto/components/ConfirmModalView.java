package dev.vepo.contraponto.components;

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
}
