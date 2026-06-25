package dev.vepo.contraponto.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthorTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@WebAuthorTest
class ImageControlTest {

    @Inject
    ContentImageMarkerService markerService;

    @Inject
    PostImageDependencyService postImageDependencyService;

    @Inject
    PostRepository postRepository;

    private User author;
    private Blog blog;

    @Test
    void listsUploadedImageAndUsageAfterDraftSave(App app) {
        var image = Given.randomCover(blog);
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(blog)
                        .withTitle("Post with image")
                        .withSlug("post-with-image")
                        .withContent("![inline](%s)".formatted(image.getUrl()))
                        .withPublished(false)
                        .persist();
        post.setContent(markerService.toStoredContent(post.getContent()));
        postRepository.save(post);
        postImageDependencyService.syncPostDependencies(post);

        app.login(author);
        app.openUserMenu()
           .clickMenuLink("/writing")
           .clickHubSection("/writing", "images")
           .assertBreadcrumb("Writing", "Images")
           .assertImageControlTitle()
           .assertImageListed(image.getFilename())
           .assertImageUsage("Post with image");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("imgauthor")
                      .withEmail("imgauthor@test.com")
                      .withName("Image Author")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
    }
}
