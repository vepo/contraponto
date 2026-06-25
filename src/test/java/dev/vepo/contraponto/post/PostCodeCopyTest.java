package dev.vepo.contraponto.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthorTest;
import dev.vepo.contraponto.user.User;

@WebAuthorTest
class PostCodeCopyTest {

    private User author;
    private Post post;

    @Test
    void publishedPostCodeBlockCopyButtonShowsCopied(App app) {
        app.access()
           .goTo(post)
           .clickFirstCodeBlockCopy()
           .assertCodeBlockCopyShowsCopied();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("codecopyauthor")
                      .withEmail("codecopyauthor@test.com")
                      .withName("Code Copy Author")
                      .withPassword("password123")
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Post with code sample")
                    .withSlug("post-with-code-sample")
                    .withFormat(Format.ASCIIDOC)
                    .withContent("""
                                 [source,java]
                                 ----
                                 System.out.println("hello");
                                 ----
                                 """)
                    .withPublished(true)
                    .persist();
    }
}
