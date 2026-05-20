package dev.vepo.contraponto.shared.i18n;

/**
 * Stable i18n keys for interface copy. Default (PT-BR) text lives in Qute
 * templates; {@code messages_en.json} and {@code messages_es.json} provide
 * translations.
 */
public final class I18nKeys {

    // Auth
    public static final String AUTH_ERROR_LOGIN_REQUIRED = "auth.error.loginRequired";

    public static final String AUTH_ERROR_INVALID_CREDENTIALS = "auth.error.invalidCredentials";
    // Toasts — blog
    public static final String TOAST_BLOG_SAVED = "toast.blog.saved";

    public static final String TOAST_BLOG_NOT_FOUND = "toast.blog.notFound";
    public static final String TOAST_BLOG_FORBIDDEN = "toast.blog.forbidden";
    // Toasts — post / write
    public static final String TOAST_POST_PUBLISHED = "toast.post.published";

    public static final String TOAST_DRAFT_SAVED = "toast.draft.saved";
    public static final String TOAST_POST_CONTENT_REQUIRED = "toast.post.contentRequired";
    public static final String TOAST_POST_TITLE_REQUIRED = "toast.post.titleRequired";
    public static final String TOAST_POST_INVALID_SLUG = "toast.post.invalidSlug";
    public static final String TOAST_POST_SLUG_EXISTS = "toast.post.slugExists";
    public static final String TOAST_POST_BLOG_REQUIRED = "toast.post.blogRequired";
    // Toasts — custom page
    public static final String TOAST_PAGE_SAVED = "toast.page.saved";

    public static final String TOAST_PAGE_DELETED = "toast.page.deleted";
    public static final String TOAST_PAGE_NOT_FOUND = "toast.page.notFound";
    public static final String TOAST_PAGE_FORBIDDEN = "toast.page.forbidden";
    public static final String TOAST_PAGE_SLUG_EXISTS = "toast.page.slugExists";
    public static final String TOAST_PAGE_BLOG_REQUIRED = "toast.page.blogRequired";
    // Toasts — user
    public static final String TOAST_USER_CREATED = "toast.user.created";

    public static final String TOAST_USER_SAVED = "toast.user.saved";
    public static final String TOAST_USER_NOT_FOUND = "toast.user.notFound";
    public static final String TOAST_USER_FORBIDDEN = "toast.user.forbidden";
    // Toasts — review / roles
    public static final String TOAST_EDITOR_FORBIDDEN = "toast.editor.forbidden";

    public static final String TOAST_ADMIN_FORBIDDEN = "toast.admin.forbidden";
    public static final String TOAST_POST_NOT_FOUND = "toast.post.notFound";
    public static final String TOAST_CANNOT_FEATURE_DRAFT = "toast.review.cannotFeatureDraft";
    // Toasts — comment
    public static final String TOAST_COMMENT_NOT_FOUND = "toast.comment.notFound";

    // Toasts — highlights
    public static final String TOAST_HIGHLIGHT_CREATED = "toast.highlight.created";

    public static final String TOAST_HIGHLIGHT_NOTE_PENDING = "toast.highlight.notePending";
    public static final String TOAST_HIGHLIGHT_NOTE_SAVED = "toast.highlight.noteSaved";
    public static final String TOAST_HIGHLIGHT_NOTE_REMOVED = "toast.highlight.noteRemoved";
    public static final String TOAST_HIGHLIGHT_REMOVED = "toast.highlight.removed";
    public static final String TOAST_HIGHLIGHT_OFFICIAL_APPROVED = "toast.highlight.officialApproved";
    public static final String TOAST_HIGHLIGHT_PROPOSAL_REJECTED = "toast.highlight.proposalRejected";
    public static final String TOAST_POST_RESPONSE_PENDING = "toast.postResponse.pending";

    // Toasts — tag
    public static final String TOAST_TAG_UPDATED = "toast.tag.updated";

    public static final String TOAST_TAG_NAME_REQUIRED = "toast.tag.nameRequired";
    public static final String TOAST_TAG_SLUG_INVALID = "toast.tag.slugInvalid";
    public static final String TOAST_TAG_SLUG_TAKEN = "toast.tag.slugTaken";
    public static final String TOAST_TAG_MISSING = "toast.tag.missing";
    // Toasts — image
    public static final String TOAST_IMAGE_UPDATED = "toast.image.updated";

    // Toasts — account
    public static final String TOAST_ACCOUNT_UPDATED = "toast.account.updated";

    public static final String TOAST_APPEARANCE_UPDATED = "toast.appearance.updated";
    // Toasts — audience
    public static final String TOAST_BLOG_NOT_FOUND_AUDIENCE = "toast.audience.blogNotFound";

    // Generic
    public static final String TOAST_GENERIC_ERROR = "toast.generic.error";

    public static final String ERROR_GENERIC_ADMIN = "error.generic.contactAdmin";

    private I18nKeys() {}
}
