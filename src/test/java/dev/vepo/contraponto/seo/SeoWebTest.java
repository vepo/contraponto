package dev.vepo.contraponto.seo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class SeoWebTest {

    @Test
    void homePageLinksToAuthorAndBlogDirectories(App app) {
        User author = Given.user()
                           .withUsername("dir-author")
                           .withEmail("dir@example.com")
                           .withName("Directory Author")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Directory Seed Post")
             .withSlug("directory-seed")
             .withContent("Content")
             .withAuthor(author)
             .persist();

        app.access()
           .assertPageSourceContains("data-hx-get=\"/authors\"")
           .assertPageSourceContains("data-hx-get=\"/explore/blogs\"");
    }

    @Test
    void htmxNavigationUpdatesDocumentTitle(App app) {
        User author = Given.user()
                           .withUsername("htmx-seo")
                           .withEmail("htmx@example.com")
                           .withName("HTMX SEO")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("HTMX SEO Post")
             .withSlug("htmx-seo-post")
             .withDescription("Description")
             .withContent("Body")
             .withAuthor(author)
             .withFeatured(true)
             .persist();

        app.access()
           .featuredCard()
           .accessPost()
           .waitForReady()
           .assertDocumentTitleContains("HTMX SEO Post");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
    }
}
