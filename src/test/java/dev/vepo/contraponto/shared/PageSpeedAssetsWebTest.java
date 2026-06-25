package dev.vepo.contraponto.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.user.User;

@WebPlatformTest
class PageSpeedAssetsWebTest {

    @Test
    void blogDirectoryOmitsWriteEditorBundle(App app) {
        User author = Given.user()
                           .withUsername("perf-blogdir")
                           .withEmail("perf-blogdir@example.com")
                           .withName("Perf Blog Dir")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Perf Blog Dir Post")
             .withSlug("perf-blog-dir-post")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        app.goToPath("/explore/blogs")
           .assertPageSourceDoesNotContain("asciidoctor.min.js");
    }

    @Test
    void homePageOmitsWriteEditorBundle(App app) {
        User author = Given.user()
                           .withUsername("perf-home")
                           .withEmail("perf-home@example.com")
                           .withName("Perf Home")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Perf Home Post")
             .withSlug("perf-home-post")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        app.access()
           .assertPageSourceContains("asset-loader.js")
           .assertPageSourceDoesNotContain("asciidoctor.min.js")
           .assertPageSourceDoesNotContain("write.js")
           .assertPageSourceDoesNotContain("manage.css")
           .assertPageSourceDoesNotContain("write.css");
    }

    @Test
    void htmxWriteNavigationLoadsEditorAfterPublicHome(App app) {
        User author = Given.user()
                           .withUsername("perf-write")
                           .withEmail("perf-write@example.com")
                           .withName("Perf Write")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Perf Write Seed")
             .withSlug("perf-write-seed")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        app.login(author)
           .clickHeaderWriteButton();
    }

    @Test
    void postPageIncludesHighlightBundle(App app) {
        User author = Given.user()
                           .withUsername("perf-post")
                           .withEmail("perf-post@example.com")
                           .withName("Perf Post")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Perf Post Title")
             .withSlug("perf-post-slug")
             .withContent("```java\nclass Example {}\n```")
             .withAuthor(author)
             .withPublished(true)
             .persist();

        app.goToPath("/perf-post/post/perf-post-slug")
           .assertPageSourceContains("highlight.min.js")
           .assertPageSourceDoesNotContain("asciidoctor.min.js");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
    }
}
