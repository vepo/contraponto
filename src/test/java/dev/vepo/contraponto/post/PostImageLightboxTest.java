package dev.vepo.contraponto.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class PostImageLightboxTest {

    private User author;
    private Post post;

    @Test
    void asciidocBlockTitleShownInLightboxCaption(App app) {
        var image = Given.randomCover(author.getDefaultBlog());
        var asciidocPost = Given.post()
                                .withAuthor(author)
                                .withTitle("AsciiDoc figure post")
                                .withSlug("asciidoc-figure-post")
                                .withFormat(Format.ASCIIDOC)
                                .withContent("""
                                             .Figure caption for lightbox
                                             image::%s[]
                                             """.formatted(image.getUrl()))
                                .withPublished(true)
                                .persist();
        app.access()
           .goTo(asciidocPost)
           .clickFirstContentImage()
           .assertImageLightboxOpen()
           .assertImageLightboxCaption("Figure caption for lightbox")
           .closeImageLightboxWithEscape();
    }

    @Test
    void publishedPostContentImageOpensAndClosesLightbox(App app) {
        app.access()
           .goTo(post)
           .clickFirstContentImage()
           .assertImageLightboxOpen()
           .assertImageLightboxCaption("Diagram alt")
           .closeImageLightboxWithEscape();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("lightboxauthor")
                      .withEmail("lightboxauthor@test.com")
                      .withName("Lightbox Author")
                      .withPassword("password123")
                      .persist();
        var image = Given.randomCover(author.getDefaultBlog());
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Post with inline image")
                    .withSlug("post-with-inline-image")
                    .withContent("![Diagram alt](%s)".formatted(image.getUrl()))
                    .withPublished(true)
                    .persist();
    }
}
