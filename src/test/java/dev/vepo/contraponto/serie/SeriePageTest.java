package dev.vepo.contraponto.serie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.tag.TagSlug;
import dev.vepo.contraponto.user.User;

@WebTest
class SeriePageTest {

    private User author;
    private Post partTwo;

    @Test
    void postPageSerieNavListsPartsInOrder(App app) {
        app.access()
           .goTo(partTwo)
           .assertSerieNavVisible("My Tutorial")
           .assertSerieNavPartCount(2)
           .assertSerieNavListsPart("Part One")
           .assertSerieNavListsPart("Part Two")
           .assertSerieNavCurrentPart("Part Two")
           .assertSerieNavLinkedPart("Part One")
           .assertSerieNavPartListedBefore("Part One", "Part Two");
    }

    @Test
    void postPageShowsSerieNav(App app) {
        Post partThree = Given.post()
                              .withTitle("Part Three")
                              .withSlug("part-three")
                              .withDescription("Third")
                              .withContent("Body text for part three.")
                              .withAuthor(author)
                              .withSerieTitle("Another Arc")
                              .persist();
        app.access()
           .goTo(partThree)
           .assertSerieNavVisible("Another Arc")
           .assertSerieNavPartCount(1)
           .assertSerieNavCurrentPart("Part Three");
    }

    @Test
    void seriePageListsPostsOldestFirst(App app) {
        app.access()
           .goToSerie("serieauthor", TagSlug.slugify("My Tutorial"))
           .assertListsPostTitle("Part One")
           .assertListsPostTitle("Part Two")
           .assertPostListedBefore("Part One", "Part Two");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("serieauthor")
                      .withEmail("serieauthor@example.com")
                      .withPassword("serieauthorpass")
                      .withName("Series Author")
                      .persist();
        String body = """
                      Content of the blog post. Lorem ipsum dolor sit amet.
                      """;
        Given.post()
             .withTitle("Part One")
             .withSlug("part-one")
             .withDescription("First")
             .withContent(body)
             .withAuthor(author)
             .withSerieTitle("My Tutorial")
             .persist();
        partTwo = Given.post()
                       .withTitle("Part Two")
                       .withSlug("part-two")
                       .withDescription("Second")
                       .withContent(body)
                       .withAuthor(author)
                       .withSerieTitle("My Tutorial")
                       .persist();
    }
}
