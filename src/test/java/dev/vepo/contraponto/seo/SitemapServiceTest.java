package dev.vepo.contraponto.seo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class SitemapServiceTest {

    @Inject
    SitemapService sitemapService;

    private User author;

    @Test
    void renderXml_includesLastmodAndImageForPostWithCover() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("Sitemap cover post")
                         .withSlug("sitemap-cover")
                         .withContent("Body for sitemap image test.")
                         .withCover(Given.randomCover(author.getDefaultBlog()))
                         .withPublished(true)
                         .persist();

        String xml = sitemapService.renderXml();
        assertThat(xml).contains("/" + author.getUsername() + "/post/" + post.getSlug());
        assertThat(xml).contains("<lastmod>");
        assertThat(xml).contains("<image:image>");
        assertThat(xml).contains("xmlns:image=");
    }

    @Test
    void renderXml_includesTagWithLastmod() {
        Given.post()
             .withAuthor(author)
             .withTitle("Tagged sitemap")
             .withSlug("tagged-sitemap")
             .withContent("Tagged body.")
             .withTags("sitemap-tag")
             .withPublished(true)
             .persist();

        String xml = sitemapService.renderXml();
        assertThat(xml).contains("/tags/sitemap-tag");
        assertThat(xml).contains("<lastmod>");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("sitemap-user")
                      .withEmail("sitemap@test.com")
                      .withName("Sitemap Author")
                      .withPassword("password123")
                      .persist();
    }
}
