package dev.vepo.contraponto.components;

import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;

enum ConfirmModalAction {

    POST_UNPUBLISH(
            "POST",
            I18nKeys.LIBRARY_UNPUBLISH,
            I18nDefaults.LIBRARY_UNPUBLISH,
            I18nKeys.LIBRARY_UNPUBLISH_CONFIRM,
            I18nDefaults.LIBRARY_UNPUBLISH_CONFIRM,
            I18nKeys.LIBRARY_UNPUBLISH,
            I18nDefaults.LIBRARY_UNPUBLISH),

    POST_DELETE(
            "DELETE",
            I18nKeys.COMMON_DELETE,
            I18nDefaults.COMMON_DELETE,
            I18nKeys.LIBRARY_DELETE_DRAFT_CONFIRM,
            I18nDefaults.LIBRARY_DELETE_DRAFT_CONFIRM,
            I18nKeys.COMMON_DELETE,
            I18nDefaults.COMMON_DELETE);

    private final String httpMethod;
    private final String titleKey;
    private final String titleDefault;
    private final String messageKey;
    private final String messageDefault;
    private final String confirmKey;
    private final String confirmDefault;

    ConfirmModalAction(String httpMethod,
                       String titleKey,
                       String titleDefault,
                       String messageKey,
                       String messageDefault,
                       String confirmKey,
                       String confirmDefault) {
        this.httpMethod = httpMethod;
        this.titleKey = titleKey;
        this.titleDefault = titleDefault;
        this.messageKey = messageKey;
        this.messageDefault = messageDefault;
        this.confirmKey = confirmKey;
        this.confirmDefault = confirmDefault;
    }

    String actionUrl(long postId) {
        return switch (this) {
            case POST_UNPUBLISH -> "/forms/posts/%s/unpublish".formatted(postId);
            case POST_DELETE -> "/forms/posts/%s".formatted(postId);
        };
    }

    String confirmDefault() {
        return confirmDefault;
    }

    String confirmKey() {
        return confirmKey;
    }

    String httpMethod() {
        return httpMethod;
    }

    String messageDefault() {
        return messageDefault;
    }

    String messageKey() {
        return messageKey;
    }

    String targetSelector(long postId) {
        return "[data-post-id='%s']".formatted(postId);
    }

    String titleDefault() {
        return titleDefault;
    }

    String titleKey() {
        return titleKey;
    }
}
