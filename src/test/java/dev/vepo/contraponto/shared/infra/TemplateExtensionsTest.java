package dev.vepo.contraponto.shared.infra;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogTemplateExtensions;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.notification.Notification;
import dev.vepo.contraponto.notification.NotificationTemplateExtensions;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostTemplateExtensions;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagTemplateExtensions;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserTemplateExtensions;
import dev.vepo.contraponto.shared.qute.SharedTemplateExtensions;

@UnitTest
class TemplateExtensionsTest {

    @Test
    void avatarUrlUsesLocalGeneratedAvatar() {
        var user = new User();
        user.setName("José Silva");
        var logged = new LoggedUser(user, "session");
        assertThat(UserTemplateExtensions.avatarUrl(logged)).startsWith("/components/avatar?name=");
        assertThat(UserTemplateExtensions.avatarUrl((LoggedUser) null)).isEmpty();
    }

    @Test
    void avatarUrlUsesStoredProfilePicture() {
        var picture = new Image();
        picture.setUrl("/api/images/profile.png");
        var user = new User();
        user.setName("Ada Lovelace");
        user.setProfilePicture(picture);
        assertThat(UserTemplateExtensions.avatarUrl(user)).isEqualTo("/api/images/profile.png");
    }

    @Test
    void blogGridLoadMorePathFormatsSecondaryBlog() {
        var owner = new dev.vepo.contraponto.user.User();
        owner.setUsername("bob");
        var blog = new dev.vepo.contraponto.blog.Blog();
        blog.setOwner(owner);
        blog.setSlug("architecture-notes");
        blog.setMain(false);
        assertThat(BlogTemplateExtensions.blogGridLoadMorePath(blog)).isEqualTo("/bob/architecture-notes/components/grid");
    }

    @Test
    void blogGridLoadMorePathFormatsUsername() {
        assertThat(BlogTemplateExtensions.blogGridLoadMorePath("alice")).isEqualTo("/alice/components/grid");
    }

    @Test
    void coverUrlFallsBackToDraftCover() {
        var cover = new dev.vepo.contraponto.image.Image();
        cover.setUrl("/api/images/draft.png");
        var post = new Post();
        post.setCover(cover);
        assertThat(PostTemplateExtensions.coverUrl(post)).isEqualTo("/api/images/draft.png");
    }

    @Test
    void coverUrlPrefersLivePublicationCover() {
        var cover = new Image();
        cover.setUrl("/api/images/cover.png");
        var live = new PostPublication();
        live.setCover(cover);
        var post = new Post();
        post.setLivePublication(live);
        assertThat(PostTemplateExtensions.coverUrl(post)).isEqualTo("/api/images/cover.png");
    }

    @Test
    void coverUrlReturnsNullWhenPostMissing() {
        assertThat(PostTemplateExtensions.coverUrl(null)).isNull();
    }

    @Test
    void escapeHtmlEscapesSpecialCharacters() {
        assertThat(SharedTemplateExtensions.escapeHtml("<a & \"'>")).isEqualTo("&lt;a &amp; &quot;&#39;&gt;");
    }

    @Test
    void escapeHtmlReturnsNullLiteralForNull() {
        assertThat(SharedTemplateExtensions.escapeHtml(null)).isEqualTo("null");
    }

    @Test
    void firstNameReturnsEmptyWhenUserMissing() {
        assertThat(UserTemplateExtensions.firstName(null)).isEmpty();
    }

    @Test
    void firstNameReturnsLeadingTokenFromFullName() {
        var user = new User();
        user.setName("Ada Lovelace");
        assertThat(UserTemplateExtensions.firstName(new LoggedUser(user, "s"))).isEqualTo("Ada");
    }

    @Test
    void formatDateFormatsTimestamp() {
        var formatted = SharedTemplateExtensions.formatDate(LocalDateTime.of(2026, 5, 17, 14, 30));
        assertThat(formatted).isEqualTo("17/05/2026 14:30");
    }

    @Test
    void formatDateReturnsEmptyForNull() {
        assertThat(SharedTemplateExtensions.formatDate(null)).isEmpty();
    }

    @Test
    void formatReadingDurationFormatsMinutesAndHours() {
        assertThat(SharedTemplateExtensions.formatReadingDuration(30)).isEqualTo("< 1 min");
        assertThat(SharedTemplateExtensions.formatReadingDuration(120)).isEqualTo("2 min");
        assertThat(SharedTemplateExtensions.formatReadingDuration(3900)).isEqualTo("1h 5m");
    }

    @Test
    void formatReadingTimeTotalFormatsMonthTotals() {
        assertThat(SharedTemplateExtensions.formatReadingTimeTotal(0)).isEqualTo("0 min");
        assertThat(SharedTemplateExtensions.formatReadingTimeTotal(5400)).isEqualTo("1h 30m");
    }

    @Test
    void linkUrlForFollowReturnsBlogUrl() {
        var owner = new User();
        owner.setUsername("bob");
        var blog = new Blog();
        blog.setName("Bob Blog");
        blog.setOwner(owner);
        blog.setMain(true);
        var notification = new Notification();
        notification.setType(NotificationType.NEW_FOLLOW);
        notification.setBlog(blog);
        assertThat(NotificationTemplateExtensions.linkUrl(notification)).isEqualTo("/bob");
    }

    @Test
    void liveContentAndPublishedAtPreferLivePublication() {
        var post = new Post();
        post.setContent("draft");
        post.setPublishedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        var live = new PostPublication();
        live.setContent("live");
        live.setPublishedAt(LocalDateTime.of(2026, 2, 1, 9, 0));
        post.setLivePublication(live);
        var view = new PublishedPostView(post, live);
        assertThat(PostTemplateExtensions.liveContent(post)).isEqualTo("live");
        assertThat(PostTemplateExtensions.livePublishedAt(view)).isEqualTo(live.getPublishedAt());
    }

    @Test
    void liveDescriptionPrefersPublicationSnapshot() {
        var post = new Post();
        post.setDescription("Draft **summary**");
        var live = new PostPublication();
        live.setDescription("Published **summary**");
        assertThat(PostTemplateExtensions.liveDescription(post)).isEqualTo("Draft **summary**");
        post.setLivePublication(live);
        assertThat(PostTemplateExtensions.liveDescription(post)).isEqualTo("Published **summary**");
    }

    @Test
    void liveTagsPreferPublicationTags() {
        var tag = new Tag();
        tag.setSlug("news");
        var post = new Post();
        post.setTags(List.of());
        var live = new PostPublication();
        live.setTags(List.of(tag));
        assertThat(PostTemplateExtensions.liveTags(new PublishedPostView(post, live))).containsExactly(tag);
    }

    @Test
    void liveTitleOnPostFallsBackToDraftTitle() {
        var post = new Post();
        post.setTitle("Draft title");
        assertThat(PostTemplateExtensions.liveTitle(post)).isEqualTo("Draft title");
    }

    @Test
    void liveTitlePrefersPublicationTitle() {
        var post = new Post();
        post.setTitle("Draft");
        var live = new PostPublication();
        live.setTitle("Published");
        assertThat(PostTemplateExtensions.liveTitle(new PublishedPostView(post, live))).isEqualTo("Published");
    }

    @Test
    void liveVersionReturnsPublicationVersion() {
        var post = new Post();
        var live = new PostPublication();
        live.setVersion(3);
        assertThat(PostTemplateExtensions.liveVersion(new PublishedPostView(post, live))).isEqualTo(3);
    }

    @Test
    void liveVersionReturnsZeroWithoutPublication() {
        assertThat(PostTemplateExtensions.liveVersion(new PublishedPostView(new Post(), null))).isZero();
    }

    @Test
    void navigationUrlForMessageThreadReturnsThreadPath() throws Exception {
        var notification = new Notification();
        notification.setType(NotificationType.NEW_MESSAGE_THREAD);
        var field = Notification.class.getDeclaredField("messageThreadId");
        field.setAccessible(true);
        field.set(notification, 7L);

        assertThat(NotificationTemplateExtensions.navigationUrl(notification)).isEqualTo("/account/messages/7");
        assertThat(NotificationTemplateExtensions.linkUrl(notification)).isEqualTo("/account/messages/7");
    }

    @Test
    void notificationMessageAndLinkForGitSyncFailed() {
        var owner = new User();
        owner.setUsername("alice");
        var blog = new Blog();
        blog.setId(42L);
        blog.setName("Alice on Systems");
        blog.setOwner(owner);
        blog.setMain(true);
        var notification = new Notification();
        notification.setType(NotificationType.GIT_SYNC_FAILED);
        notification.setBlog(blog);

        assertThat(NotificationTemplateExtensions.message(notification)).isEqualTo("Git sync failed for Alice on Systems");
    }

    @Test
    void notificationMessageAndLinkForHighlightTypes() {
        var owner = new User();
        owner.setUsername("alice");
        var blog = new Blog();
        blog.setName("Alice Blog");
        blog.setOwner(owner);
        blog.setMain(true);
        var post = new Post();
        post.setTitle("Highlight Post");
        post.setSlug("highlight-post");
        post.setBlog(blog);
        var actor = new User();
        actor.setName("Reader");

        var proposal = new Notification();
        proposal.setType(NotificationType.COMMON_HIGHLIGHT_PROPOSAL);
        proposal.setBlog(blog);
        proposal.setPost(post);
        assertThat(NotificationTemplateExtensions.message(proposal)).contains("highlighted");
        assertThat(NotificationTemplateExtensions.linkUrl(proposal)).isEqualTo("/writing/highlights");

        var publicNote = new Notification();
        publicNote.setType(NotificationType.PUBLIC_HIGHLIGHT_NOTE);
        publicNote.setBlog(blog);
        publicNote.setPost(post);
        publicNote.setActor(actor);
        assertThat(NotificationTemplateExtensions.message(publicNote)).contains("public highlight note");
        assertThat(NotificationTemplateExtensions.linkUrl(publicNote)).isEqualTo("/writing/highlights");

        var response = new Notification();
        response.setType(NotificationType.POST_RESPONSE);
        response.setBlog(blog);
        response.setPost(post);
        response.setActor(actor);
        assertThat(NotificationTemplateExtensions.message(response)).contains("response");
        assertThat(NotificationTemplateExtensions.linkUrl(response)).isEqualTo("/writing/highlights");
    }

    @Test
    void notificationMessageAndLinkForNewComment() {
        var owner = new User();
        owner.setUsername("bob");
        var actor = new User();
        actor.setName("Alice");
        var blog = new Blog();
        blog.setName("Bob Blog");
        blog.setOwner(owner);
        blog.setMain(true);
        var post = new Post();
        post.setSlug("launch");
        post.setBlog(blog);
        var notification = new Notification();
        notification.setType(NotificationType.NEW_COMMENT);
        notification.setBlog(blog);
        notification.setPost(post);
        notification.setActor(actor);

        assertThat(NotificationTemplateExtensions.message(notification)).isEqualTo("Alice commented on launch");
        assertThat(NotificationTemplateExtensions.linkUrl(notification)).isEqualTo("/bob/post/launch#comments");
    }

    @Test
    void notificationMessageAndLinkForNewPost() {
        var owner = new User();
        owner.setUsername("bob");
        var blog = new Blog();
        blog.setName("Bob Blog");
        blog.setOwner(owner);
        blog.setMain(true);
        var post = new Post();
        post.setTitle("Launch");
        post.setSlug("launch");
        post.setBlog(blog);
        var notification = new Notification();
        notification.setType(NotificationType.NEW_POST);
        notification.setBlog(blog);
        notification.setPost(post);

        assertThat(NotificationTemplateExtensions.message(notification)).isEqualTo("Bob Blog published Launch");
        assertThat(NotificationTemplateExtensions.linkUrl(notification)).isEqualTo("/bob/post/launch");
    }

    @Test
    void notificationMessageForFollowAndSubscribe() {
        var blog = new Blog();
        blog.setName("Studio");
        var actor = new User();
        actor.setName("Fan");
        var follow = new Notification();
        follow.setType(NotificationType.NEW_FOLLOW);
        follow.setBlog(blog);
        follow.setActor(actor);
        assertThat(NotificationTemplateExtensions.message(follow)).isEqualTo("Fan started following Studio");

        var subscribe = new Notification();
        subscribe.setType(NotificationType.NEW_SUBSCRIBE);
        subscribe.setBlog(blog);
        subscribe.setActor(actor);
        assertThat(NotificationTemplateExtensions.message(subscribe)).isEqualTo("Fan subscribed by email to Studio");
    }

    @Test
    void notificationMessageUsesSlugWhenTitleBlank() {
        var owner = new User();
        owner.setUsername("bob");
        var blog = new Blog();
        blog.setName("Bob Blog");
        blog.setOwner(owner);
        blog.setMain(true);
        var post = new Post();
        post.setTitle("   ");
        post.setSlug("quiet-launch");
        post.setBlog(blog);
        var notification = new Notification();
        notification.setType(NotificationType.NEW_POST);
        notification.setBlog(blog);
        notification.setPost(post);
        assertThat(NotificationTemplateExtensions.message(notification)).isEqualTo("Bob Blog published quiet-launch");
    }

    @Test
    void readTimeReturnsOneMinuteForBriefContent() {
        assertThat(SharedTemplateExtensions.readTime("hello")).isEqualTo("1 min read");
    }

    @Test
    void readTimeReturnsZeroMinutesForEmptyContent() {
        assertThat(SharedTemplateExtensions.readTime(null)).isEqualTo("0 min read");
        assertThat(SharedTemplateExtensions.readTime("   ")).isEqualTo("0 min read");
    }

    @Test
    void readTimeRoundsUpWordCount() {
        String words = "word ".repeat(250).trim();
        assertThat(SharedTemplateExtensions.readTime(words)).isEqualTo("2 min read");
    }

    @Test
    void showUpdatedWhenLiveVersionGreaterThanOne() {
        var post = new Post();
        var live = new PostPublication();
        live.setVersion(2);
        assertThat(PostTemplateExtensions.showUpdated(new PublishedPostView(post, live))).isTrue();
    }

    @Test
    void tagGridLoadMorePathFormatsSlug() {
        assertThat(TagTemplateExtensions.tagGridLoadMorePath("java")).isEqualTo("/tags/java/components/grid");
    }
}
