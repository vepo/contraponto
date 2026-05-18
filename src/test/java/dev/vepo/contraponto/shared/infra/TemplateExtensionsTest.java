package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.notification.Notification;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.user.User;

class TemplateExtensionsTest {

    @Test
    void avatarUrlEncodesUserName() {
        var user = new User();
        user.setName("José Silva");
        var logged = new LoggedUser(user, "session");
        assertThat(TemplateExtensions.avatarUrl(logged)).contains("name=");
        assertThat(TemplateExtensions.avatarUrl(null)).isEmpty();
    }

    @Test
    void blogGridLoadMorePathFormatsUsername() {
        assertThat(TemplateExtensions.blogGridLoadMorePath("alice")).isEqualTo("/alice/components/grid");
    }

    @Test
    void coverUrlFallsBackToDraftCover() {
        var cover = new dev.vepo.contraponto.image.Image();
        cover.setUrl("/api/images/draft.png");
        var post = new Post();
        post.setCover(cover);
        assertThat(TemplateExtensions.coverUrl(post)).isEqualTo("/api/images/draft.png");
    }

    @Test
    void coverUrlPrefersLivePublicationCover() {
        var cover = new Image();
        cover.setUrl("/api/images/cover.png");
        var live = new PostPublication();
        live.setCover(cover);
        var post = new Post();
        post.setLivePublication(live);
        assertThat(TemplateExtensions.coverUrl(post)).isEqualTo("/api/images/cover.png");
    }

    @Test
    void coverUrlReturnsNullWhenPostMissing() {
        assertThat(TemplateExtensions.coverUrl(null)).isNull();
    }

    @Test
    void escapeHtmlEscapesSpecialCharacters() {
        assertThat(TemplateExtensions.escapeHtml("<a & \"'>")).isEqualTo("&lt;a &amp; &quot;&#39;&gt;");
    }

    @Test
    void escapeHtmlReturnsNullLiteralForNull() {
        assertThat(TemplateExtensions.escapeHtml(null)).isEqualTo("null");
    }

    @Test
    void firstNameReturnsEmptyWhenUserMissing() {
        assertThat(TemplateExtensions.firstName(null)).isEmpty();
    }

    @Test
    void firstNameReturnsLeadingTokenFromFullName() {
        var user = new User();
        user.setName("Ada Lovelace");
        assertThat(TemplateExtensions.firstName(new LoggedUser(user, "s"))).isEqualTo("Ada");
    }

    @Test
    void formatDateFormatsTimestamp() {
        var formatted = TemplateExtensions.formatDate(LocalDateTime.of(2026, 5, 17, 14, 30));
        assertThat(formatted).isEqualTo("17/05/2026 14:30");
    }

    @Test
    void formatDateReturnsEmptyForNull() {
        assertThat(TemplateExtensions.formatDate(null)).isEmpty();
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
        assertThat(TemplateExtensions.linkUrl(notification)).isEqualTo("/bob");
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
        assertThat(TemplateExtensions.liveContent(post)).isEqualTo("live");
        assertThat(TemplateExtensions.livePublishedAt(view)).isEqualTo(live.getPublishedAt());
    }

    @Test
    void liveTagsPreferPublicationTags() {
        var tag = new Tag();
        tag.setSlug("news");
        var post = new Post();
        post.setTags(List.of());
        var live = new PostPublication();
        live.setTags(List.of(tag));
        assertThat(TemplateExtensions.liveTags(new PublishedPostView(post, live))).containsExactly(tag);
    }

    @Test
    void liveTitleOnPostFallsBackToDraftTitle() {
        var post = new Post();
        post.setTitle("Draft title");
        assertThat(TemplateExtensions.liveTitle(post)).isEqualTo("Draft title");
    }

    @Test
    void liveTitlePrefersPublicationTitle() {
        var post = new Post();
        post.setTitle("Draft");
        var live = new PostPublication();
        live.setTitle("Published");
        assertThat(TemplateExtensions.liveTitle(new PublishedPostView(post, live))).isEqualTo("Published");
    }

    @Test
    void liveVersionReturnsPublicationVersion() {
        var post = new Post();
        var live = new PostPublication();
        live.setVersion(3);
        assertThat(TemplateExtensions.liveVersion(new PublishedPostView(post, live))).isEqualTo(3);
    }

    @Test
    void liveVersionReturnsZeroWithoutPublication() {
        assertThat(TemplateExtensions.liveVersion(new PublishedPostView(new Post(), null))).isZero();
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

        assertThat(TemplateExtensions.message(notification)).isEqualTo("Alice commented on launch");
        assertThat(TemplateExtensions.linkUrl(notification)).isEqualTo("/bob/post/launch#comments");
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

        assertThat(TemplateExtensions.message(notification)).isEqualTo("Bob Blog published Launch");
        assertThat(TemplateExtensions.linkUrl(notification)).isEqualTo("/bob/post/launch");
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
        assertThat(TemplateExtensions.message(follow)).isEqualTo("Fan started following Studio");

        var subscribe = new Notification();
        subscribe.setType(NotificationType.NEW_SUBSCRIBE);
        subscribe.setBlog(blog);
        subscribe.setActor(actor);
        assertThat(TemplateExtensions.message(subscribe)).isEqualTo("Fan subscribed by email to Studio");
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
        assertThat(TemplateExtensions.message(notification)).isEqualTo("Bob Blog published quiet-launch");
    }

    @Test
    void readTimeReturnsOneMinuteForBriefContent() {
        assertThat(TemplateExtensions.readTime("hello")).isEqualTo("1 min read");
    }

    @Test
    void readTimeReturnsZeroMinutesForEmptyContent() {
        assertThat(TemplateExtensions.readTime(null)).isEqualTo("0 min read");
        assertThat(TemplateExtensions.readTime("   ")).isEqualTo("0 min read");
    }

    @Test
    void readTimeRoundsUpWordCount() {
        String words = "word ".repeat(250).trim();
        assertThat(TemplateExtensions.readTime(words)).isEqualTo("2 min read");
    }

    @Test
    void showUpdatedWhenLiveVersionGreaterThanOne() {
        var post = new Post();
        var live = new PostPublication();
        live.setVersion(2);
        assertThat(TemplateExtensions.showUpdated(new PublishedPostView(post, live))).isTrue();
    }

    @Test
    void tagGridLoadMorePathFormatsSlug() {
        assertThat(TemplateExtensions.tagGridLoadMorePath("java")).isEqualTo("/tags/java/components/grid");
    }
}
