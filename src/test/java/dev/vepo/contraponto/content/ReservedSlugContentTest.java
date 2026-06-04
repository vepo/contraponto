package dev.vepo.contraponto.content;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.custompage.PagePlacement;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestTags;
import dev.vepo.contraponto.tag.TagSlug;
import dev.vepo.contraponto.user.Role;

/**
 * Ensures route-reserved path segments do not block post or custom-page slugs.
 */
@QuarkusIntegrationTest
@Tag(TestTags.RESERVED_SLUGS)
class ReservedSlugContentTest {

    private static List<String> reservedWordsValidAsPostSlug() {
        var words = new ArrayList<String>();
        for (String segment : CustomPagePaths.reservedSegments()) {
            if (!TagSlug.hasInvalidSlugCharacters(segment)) {
                words.add(segment);
            }
        }
        return words;
    }

    @AfterEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    void publishedPostsAndCustomPagesAcceptEveryReservedWordAsSlug() {
        var author = Given.user()
                          .withUsername("rslugauth")
                          .withEmail("rslugauth@example.com")
                          .withPassword("ReservedSlugPass1")
                          .withName("Reserved Slug Author")
                          .withRole(Role.USER)
                          .persist();
        var blog = author.getDefaultBlog();

        for (String slug : reservedWordsValidAsPostSlug()) {
            var post = Given.post()
                            .withAuthor(author)
                            .withBlog(blog)
                            .withSlug(slug)
                            .withTitle("Post " + slug)
                            .withDescription("Description for " + slug)
                            .withContent("# " + slug)
                            .withFormat(Format.MARKDOWN)
                            .withPublished(true)
                            .persist();

            given().get(PostEndpoint.extractUrl(post))
                   .then()
                   .statusCode(200)
                   .body(containsString(post.getTitle()));

            var page = Given.customPage()
                            .withBlog(blog)
                            .withSlug("/" + slug)
                            .withTitle("Page " + slug)
                            .withSection("Info")
                            .withContent("<p>" + slug + "</p>")
                            .withPlacement(PagePlacement.FOOTER)
                            .persist();

            given().get(CustomPagePaths.publicUrl(page))
                   .then()
                   .statusCode(200)
                   .body(containsString(page.getTitle()));
        }

        assertThat(reservedWordsValidAsPostSlug()).isNotEmpty();
        assertThat(reservedWordsValidAsPostSlug()).contains("js", "style", "post", "pages");
    }
}
