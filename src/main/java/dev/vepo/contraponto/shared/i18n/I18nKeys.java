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

    public static final String AUTH_SIGNUP_ACTIVATION_SENT = "auth.signupActivationSent";

    public static final String ACCOUNT_REPORT_SIGNUP_CONFIRMED = "accountReportSignup.confirmed";

    // Account emails
    public static final String ACCOUNT_EMAIL_GREETING = "accountEmail.greeting";

    public static final String ACCOUNT_EMAIL_FOOTER_SECURITY = "accountEmail.footer.security";

    public static final String ACCOUNT_EMAIL_ACTIVATION_SUBJECT = "accountEmail.activation.subject";

    public static final String ACCOUNT_EMAIL_ACTIVATION_TITLE = "accountEmail.activation.title";

    public static final String ACCOUNT_EMAIL_ACTIVATION_BODY = "accountEmail.activation.body";

    public static final String ACCOUNT_EMAIL_ACTIVATION_ACTION = "accountEmail.activation.action";

    public static final String ACCOUNT_EMAIL_ACTIVATION_EXPIRY = "accountEmail.activation.expiry";

    public static final String ACCOUNT_EMAIL_ACTIVATION_REPORT = "accountEmail.activation.report";

    public static final String ACCOUNT_EMAIL_ACTIVATION_FOOTER = "accountEmail.activation.footer";

    public static final String ACCOUNT_EMAIL_PASSWORD_RESET_SUBJECT = "accountEmail.passwordReset.subject";

    public static final String ACCOUNT_EMAIL_PASSWORD_RESET_TITLE = "accountEmail.passwordReset.title";

    public static final String ACCOUNT_EMAIL_PASSWORD_RESET_BODY = "accountEmail.passwordReset.body";

    public static final String ACCOUNT_EMAIL_PASSWORD_RESET_ACTION = "accountEmail.passwordReset.action";

    public static final String ACCOUNT_EMAIL_PASSWORD_RESET_EXPIRY = "accountEmail.passwordReset.expiry";

    public static final String ACCOUNT_EMAIL_PASSWORD_RESET_FOOTER = "accountEmail.passwordReset.footer";

    public static final String ACCOUNT_EMAIL_PASSWORD_CHANGED_SUBJECT = "accountEmail.passwordChanged.subject";

    public static final String ACCOUNT_EMAIL_PASSWORD_CHANGED_TITLE = "accountEmail.passwordChanged.title";

    public static final String ACCOUNT_EMAIL_PASSWORD_CHANGED_BODY = "accountEmail.passwordChanged.body";

    public static final String ACCOUNT_EMAIL_PASSWORD_CHANGED_WARNING = "accountEmail.passwordChanged.warning";

    public static final String ACCOUNT_EMAIL_PASSWORD_CHANGED_FOOTER = "accountEmail.passwordChanged.footer";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_SUBJECT = "accountEmail.emailChangeVerification.subject";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_TITLE = "accountEmail.emailChangeVerification.title";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_BODY = "accountEmail.emailChangeVerification.body";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_ACTION = "accountEmail.emailChangeVerification.action";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_EXPIRY = "accountEmail.emailChangeVerification.expiry";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_FOOTER = "accountEmail.emailChangeVerification.footer";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGED_SUBJECT = "accountEmail.emailChanged.subject";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGED_TITLE = "accountEmail.emailChanged.title";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGED_BODY = "accountEmail.emailChanged.body";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGED_WARNING = "accountEmail.emailChanged.warning";

    public static final String ACCOUNT_EMAIL_EMAIL_CHANGED_FOOTER = "accountEmail.emailChanged.footer";
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
    public static final String TOAST_POST_SLUG_REQUIRED = "toast.post.slugRequired";
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
    public static final String TOAST_POST_UNPUBLISHED = "toast.post.unpublished";
    public static final String TOAST_POST_DELETED = "toast.post.deleted";
    public static final String TOAST_POST_DELETE_PUBLISHED = "toast.post.deletePublished";
    public static final String TOAST_POST_ALREADY_DRAFT = "toast.post.alreadyDraft";
    public static final String TOAST_POST_FORBIDDEN = "toast.post.forbidden";
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

    // Toasts — reading list
    public static final String TOAST_READING_LIST_SAVED = "toast.readingList.saved";
    public static final String TOAST_READING_LIST_ALREADY_SAVED = "toast.readingList.alreadySaved";
    public static final String TOAST_READING_LIST_MARKED_READ = "toast.readingList.markedRead";
    public static final String TOAST_READING_LIST_MARKED_UNREAD = "toast.readingList.markedUnread";
    public static final String TOAST_READING_LIST_REMOVED = "toast.readingList.removed";
    public static final String TOAST_READING_LIST_LIMIT_REACHED = "toast.readingList.limitReached";

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

    public static final String COMMON_DELETE = "common.delete";
    public static final String LIBRARY_UNPUBLISH = "library.unpublish";
    public static final String LIBRARY_UNPUBLISH_CONFIRM = "library.unpublishConfirm";
    public static final String LIBRARY_DELETE_DRAFT_CONFIRM = "library.deleteDraftConfirm";

    // Messaging
    public static final String MESSAGING_THREAD_SENT = "toast.messaging.threadSent";
    public static final String MESSAGING_REPLY_SENT = "toast.messaging.replySent";
    public static final String MESSAGING_THREAD_CLOSED = "toast.messaging.threadClosed";
    public static final String MESSAGING_THREAD_FLAGGED = "toast.messaging.threadFlagged";
    public static final String MESSAGING_USER_BLOCKED = "toast.messaging.userBlocked";
    public static final String MESSAGING_USER_UNBLOCKED = "toast.messaging.userUnblocked";
    public static final String MESSAGING_USER_NOT_FOUND = "toast.messaging.userNotFound";
    public static final String MESSAGING_THREAD_NOT_FOUND = "toast.messaging.threadNotFound";
    public static final String MESSAGING_REPORT_DISMISSED = "toast.messaging.reportDismissed";
    public static final String MESSAGING_REPORT_REVIEWED = "toast.messaging.reportReviewed";
    public static final String MESSAGING_REPORT_NOT_FOUND = "toast.messaging.reportNotFound";
    public static final String MESSAGING_CLOSE_THREAD = "messaging.closeThread";
    public static final String MESSAGING_FLAG_THREAD = "messaging.flagThread";
    public static final String MESSAGING_BLOCK_USER = "messaging.blockUser";
    public static final String MESSAGING_CLOSE_CONFIRM_TITLE = "messaging.closeConfirm.title";
    public static final String MESSAGING_CLOSE_CONFIRM_MESSAGE = "messaging.closeConfirm.message";
    public static final String MESSAGING_FLAG_CONFIRM_TITLE = "messaging.flagConfirm.title";
    public static final String MESSAGING_FLAG_CONFIRM_MESSAGE = "messaging.flagConfirm.message";
    public static final String MESSAGING_BLOCK_CONFIRM_TITLE = "messaging.blockConfirm.title";
    public static final String MESSAGING_BLOCK_CONFIRM_MESSAGE = "messaging.blockConfirm.message";

    private I18nKeys() {}
}
