package dev.vepo.contraponto.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebPlatformTest;
import dev.vepo.contraponto.user.User;

@WebPlatformTest
class CommentManageTest {

    private User author;
    private User reader;
    private Post post;

    @Test
    void authorApprovesPendingCommentFromManagePage(App app) {
        Given.inject(dev.vepo.contraponto.user.LoggedUserProvider.class).login(reader);
        Given.transaction(() -> {
            var comment = new PostComment();
            comment.setPost(post);
            comment.setAuthor(reader);
            comment.setBody("Please approve me");
            comment.setStatus(CommentStatus.PENDING);
            Given.inject(PostCommentRepository.class).save(comment);
        });

        app.login(author)
           .comments()
           .assertTitle("Manage Comments")
           .assertPendingComment("Please approve me")
           .clickApprove("Please approve me")
           .assertToastSuccess("Comment approved.")
           .assertEmptyState();
    }

    @Test
    void authorRejectsPendingCommentFromManagePage(App app) {
        Given.transaction(() -> {
            var comment = new PostComment();
            comment.setPost(post);
            comment.setAuthor(reader);
            comment.setBody("Spam comment");
            comment.setStatus(CommentStatus.PENDING);
            Given.inject(PostCommentRepository.class).save(comment);
        });

        app.login(author)
           .comments()
           .assertPendingComment("Spam comment")
           .clickReject("Spam comment")
           .assertToastSuccess("Comment rejected.")
           .assertCommentNotListed("Spam comment");
    }

    @Test
    void managePageShowsEmptyWhenNoPendingComments(App app) {
        app.login(author)
           .comments()
           .assertTitle("Manage Comments")
           .assertEmptyState();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("commentauthor")
                      .withEmail("author@example.com")
                      .withPassword("authorPw1")
                      .withName("Comment Author")
                      .persist();
        reader = Given.user()
                      .withUsername("commentreader")
                      .withEmail("reader@example.com")
                      .withPassword("readerPw1")
                      .withName("Comment Reader")
                      .persist();
        post = Given.post()
                    .withTitle("Commented Post")
                    .withSlug("commented-post")
                    .withDescription("d")
                    .withContent("body")
                    .withAuthor(author)
                    .persist();
    }

    @Test
    void unauthenticatedUserCannotAccessCommentManage(App app) {
        app.access()
           .comments()
           .assertManagePageNotLoaded();
    }
}
