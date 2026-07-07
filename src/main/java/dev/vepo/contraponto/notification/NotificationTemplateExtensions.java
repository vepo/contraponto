package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.BlogPublicUrlService;
import io.quarkus.qute.TemplateExtension;
import jakarta.enterprise.inject.spi.CDI;

@TemplateExtension
public class NotificationTemplateExtensions {

    private static final String DEFAULT_POST_TITLE = "a post";
    private static final String DEFAULT_ACTOR_NAME = "Someone";

    private static final String PLATFORM_WORKSPACE_PATH = "^/(writing|reading|manage|account|editor|administration|write|blogs)(/|$).*";

    private static boolean isPlatformWorkspacePath(String path) {
        return path != null && !path.isBlank() && path.matches(PLATFORM_WORKSPACE_PATH);
    }

    public static String linkUrl(Notification notification) {
        return navigationUrl(notification);
    }

    @TemplateExtension
    public static String message(Notification notification) {
        String blogName = notification.getBlog() != null ? notification.getBlog().getName() : "";
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
            case NEW_MESSAGE_THREAD -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getMessageThread() != null ? notification.getMessageThread().getTitle() : "a thread";
                yield "%s started a message thread: %s".formatted(actor, title);
            }
            case NEW_THREAD_MESSAGE -> {
                String actor = notification.getActor() != null ? notification.getActor().getName() : DEFAULT_ACTOR_NAME;
                String title = notification.getMessageThread() != null ? notification.getMessageThread().getTitle() : "a thread";
                yield "%s replied in %s".formatted(actor, title);
            }
        };
    }

    /**
     * Named {@code navigationUrl} (not {@code linkUrl}) so Qute does not resolve
     * {@code {n.linkUrl}} via {@link NotificationType#linkUrl(Notification)} on
     * {@code n.type}.
     */
    @TemplateExtension
    public static String navigationUrl(Notification notification) {
        String path = notification.getType().linkUrl(notification);
        var service = selectPublicUrlService();
        if (service != null && isPlatformWorkspacePath(path)) {
            return service.workspaceMenuUrl(path);
        }
        return path;
    }

    @TemplateExtension
    public static boolean navigationUsesHtmx(Notification notification) {
        var service = selectPublicUrlService();
        if (service == null || !service.usesPlatformForWorkspaceLinks()) {
            return true;
        }
        return !isPlatformWorkspacePath(notification.getType().linkUrl(notification));
    }

    private static BlogPublicUrlService selectPublicUrlService() {
        try {
            var service = CDI.current().select(BlogPublicUrlService.class);
            return service.isResolvable() ? service.get() : null;
        } catch (IllegalStateException _) {
            return null;
        }
    }

    private NotificationTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
