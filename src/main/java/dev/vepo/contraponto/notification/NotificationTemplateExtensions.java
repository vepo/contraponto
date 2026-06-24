package dev.vepo.contraponto.notification;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class NotificationTemplateExtensions {

    private static final String DEFAULT_POST_TITLE = "a post";
    private static final String DEFAULT_ACTOR_NAME = "Someone";

    @TemplateExtension
    public static String linkUrl(Notification notification) {
        return notification.getType().linkUrl(notification);
    }

    @TemplateExtension
    public static String message(Notification notification) {
        String blogName = notification.getBlog().getName();
        return switch (notification.getType()) {
            case NEW_POST -> {
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : DEFAULT_POST_TITLE;
                }
                yield "%s published %s".formatted(blogName, title);
            }
            case NEW_FOLLOW -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                yield "%s started following %s".formatted(actor, blogName);
            }
            case NEW_SUBSCRIBE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                yield "%s subscribed by email to %s".formatted(actor, blogName);
            }
            case NEW_COMMENT -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                if (title == null || title.isBlank()) {
                    title = notification.getPost() != null ? notification.getPost().getSlug() : DEFAULT_POST_TITLE;
                }
                yield "%s commented on %s".formatted(actor, title);
            }
            case COMMON_HIGHLIGHT_PROPOSAL -> {
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                yield "Readers often highlighted a passage on %s".formatted(title);
            }
            case PUBLIC_HIGHLIGHT_NOTE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                yield "%s submitted a public highlight note on %s".formatted(actor, title);
            }
            case POST_RESPONSE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getPost() != null ? notification.getPost().getTitle() : DEFAULT_POST_TITLE;
                yield "%s published a response to %s".formatted(actor, title);
            }
            case GIT_SYNC_SUCCEEDED -> "Git sync succeeded for %s".formatted(blogName);
            case GIT_SYNC_FAILED -> "Git sync failed for %s".formatted(blogName);
        };
    }

    private NotificationTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
