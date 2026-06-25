package dev.vepo.contraponto.directory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebReaderTest;
import dev.vepo.contraponto.user.User;

@WebReaderTest
class ExploreDirectoryWebTest {

    @Test
    void authorDirectoryLinksToProfile(App app) {
        User author = Given.user()
                           .withUsername("dir-profile")
                           .withEmail("dir-profile@example.com")
                           .withName("Directory Profile")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Dir Post")
             .withSlug("dir-post")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        app.goToPath("/authors")
           .assertPageSourceContains("author-directory-card")
           .assertPageSourceContains("/authors/dir-profile");
    }

    @Test
    void blogsDirectoryCardsExposeHrefForModifierClicks(App app) {
        User author = Given.user()
                           .withUsername("blogdir")
                           .withEmail("blogdir@example.com")
                           .withName("Blog Dir Author")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Blog Dir Post")
             .withSlug("blog-dir-post")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        app.goToPath("/explore/blogs")
           .assertPageSourceContains("blog-directory-card__link")
           .assertPageSourceContains("href=\"/blogdir\"")
           .assertPageSourceContains("data-hx-get=\"/blogdir\"");
    }

    @Test
    void homeShowsExploreCards(App app) {
        User author = Given.user()
                           .withUsername("explore-user")
                           .withEmail("explore@example.com")
                           .withName("Explore User")
                           .withPassword("pass12345")
                           .persist();
        Given.post()
             .withTitle("Explore Seed")
             .withSlug("explore-seed")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        app.access()
           .assertPageSourceContains("browse-explore-aside__link")
           .assertPageSourceContains("href=\"/authors\"")
           .assertPageSourceContains("data-hx-get=\"/authors\"")
           .assertPageSourceContains("href=\"/explore/blogs\"")
           .assertPageSourceContains("data-hx-get=\"/explore/blogs\"");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
    }
}
