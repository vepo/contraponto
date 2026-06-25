package dev.vepo.contraponto.seo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;

@WebReaderTest
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
           .assertPageSourceContains("browse-explore-aside__link")
           .assertPageSourceContains("href=\"/authors\"")
           .assertPageSourceContains("data-hx-get=\"/authors\"")
           .assertPageSourceContains("href=\"/explore/blogs\"")
           .assertPageSourceContains("data-hx-get=\"/explore/blogs\"")
           .assertPageSourceContains("<meta name=\"description\"");
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

    @Test
    void notFoundPageHasMetaDescriptionAndCrawlableHomeLink(App app) {
        app.goToPath("/no-such-page-for-seo-test");

        app.assertPageSourceContains("<meta name=\"description\"")
           .assertPageSourceContains("href=\"/\"")
           .assertPageSourceContains("data-hx-get=\"/\"");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
    }
}
