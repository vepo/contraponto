package dev.vepo.contraponto.seo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.navigation.BreadcrumbItem;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class StructuredDataBuilderTest {

    @Inject
    StructuredDataBuilder structuredData;

    private User author;

    @Test
    void blogPosting_includesBreadcrumbListAndDateModifiedOnRepublish() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("Schema post")
                         .withSlug("schema-post")
                         .withContent("Version one")
                         .withPublished(true)
                         .persist();
        post.setContent("Version two");
        Given.inject(dev.vepo.contraponto.post.PostPublicationService.class).publish(post);
        post = Given.inject(dev.vepo.contraponto.post.PostRepository.class)
                    .findByIdWithTags(post.getId())
                    .orElseThrow();

        var breadcrumb = new BreadcrumbTrail(List.of(
                                                     new BreadcrumbItem("Home", "/"),
                                                     new BreadcrumbItem("Schema post", null)));
        PublishedPostView view = new PublishedPostView(post, post.getLivePublication());
        String json = structuredData.blogPosting(view, breadcrumb);

        assertThat(json).contains("BreadcrumbList");
        assertThat(json).contains("dateModified");
        assertThat(json).contains("@graph");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("schema-user")
                      .withEmail("schema@test.com")
                      .withName("Schema Author")
                      .withPassword("password123")
                      .persist();
    }

    @Test
    void webSite_includesSearchAction() {
        String json = structuredData.webSite();
        assertThat(json).contains("SearchAction");
        assertThat(json).contains("search_term_string");
        assertThat(json).contains("/search?q=");
    }
}
