package dev.vepo.contraponto.seo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.navigation.BreadcrumbItem;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagRepository;
import dev.vepo.contraponto.tag.TagService;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

import java.util.List;

@QuarkusIntegrationTest
class SeoServiceTest {

    @Inject
    SeoService seoService;

    @Inject
    TagService tagService;

    @Inject
    TagRepository tagRepository;

    @Inject
    PostPublicationService publicationService;

    @Inject
    PostRepository postRepository;

    private User author;
    private Blog secondaryBlog;

    @Test
    void forAuthorDirectory_listsAuthors() {
        SeoMetadata meta = seoService.forAuthorDirectory();
        assertThat(meta.title()).contains("Autores");
        assertThat(meta.canonicalUrl()).endsWith("/authors");
        assertThat(meta.noindex()).isFalse();
    }

    @Test
    void forAuthorProfile_usesAuthorDescription() {
        author.setProfileDescription("Profile **bio** text");
        SeoMetadata meta = seoService.forAuthorProfile(author, author.getDefaultBlog());
        assertThat(meta.title()).contains(author.getName());
        assertThat(meta.description()).contains("Profile");
        assertThat(meta.ogType()).isEqualTo(SeoOgType.PROFILE);
    }

    @Test
    void forBlogDirectory_listsBlogs() {
        SeoMetadata meta = seoService.forBlogDirectory();
        assertThat(meta.title()).contains("Blogs");
        assertThat(meta.canonicalUrl()).endsWith("/explore/blogs");
    }

    @Test
    void forBlogHome_secondaryBlogUsesBlogName() {
        SeoMetadata meta = seoService.forBlogHome(author, secondaryBlog);
        assertThat(meta.title()).contains("Notes Blog");
        assertThat(meta.canonicalUrl()).contains("/seo-svc/notes");
    }

    @Test
    void forHome_hasSiteTitleAndDescription() {
        SeoMetadata meta = seoService.forHome();
        assertThat(meta.title()).contains("Contraponto");
        assertThat(meta.description()).isNotBlank();
        assertThat(meta.noindex()).isFalse();
    }

    @Test
    void forPost_usesPublicationMetadata() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("Published SEO title")
                         .withContent("Article body for SEO metadata.")
                         .withDescription("Short meta description")
                         .withSlug("published-seo")
                         .withPublished(true)
                         .persist();
        PublishedPostView view = new PublishedPostView(post, post.getLivePublication());

        SeoMetadata meta = seoService.forPost(view);
        assertThat(meta.title()).contains("Published SEO title");
        assertThat(meta.description()).isNotBlank();
        assertThat(meta.articlePublishedAt()).isPresent();
    }

    @Test
    void forPost_withBreadcrumbIncludesGraphAndModifiedAtOnRepublish() {
        Post published = Given.post()
                              .withAuthor(author)
                              .withTitle("Republish SEO")
                              .withContent("Version one")
                              .withSlug("republish-seo")
                              .withPublished(true)
                              .persist();
        long postId = published.getId();
        Given.transaction(() -> {
            Post reloaded = postRepository.findByIdWithTags(postId).orElseThrow();
            reloaded.setContent("Version two");
            publicationService.publish(reloaded);
        });
        Post post = postRepository.findByIdWithTags(postId).orElseThrow();
        PublishedPostView view = new PublishedPostView(post, post.getLivePublication());
        var breadcrumb = new BreadcrumbTrail(List.of(
                                                     new BreadcrumbItem("Home", "/"),
                                                     new BreadcrumbItem("Republish SEO", null)));

        SeoMetadata meta = seoService.forPost(view, breadcrumb);
        assertThat(meta.jsonLd().orElse("")).contains("BreadcrumbList");
        assertThat(meta.jsonLd().orElse("")).contains("@graph");
        assertThat(meta.articleModifiedAt()).isPresent();
    }

    @Test
    void forPrivatePage_readingHubIsNoindex() {
        SeoMetadata meta = seoService.forPrivatePage("Reading");
        assertThat(meta.title()).contains("Reading");
        assertThat(meta.noindex()).isTrue();
    }

    @Test
    void forSearch_blankQueryUsesDefaultTitle() {
        SeoMetadata meta = seoService.forSearch("   ");
        assertThat(meta.title()).contains("Busca");
    }

    @Test
    void forSearch_usesQueryInTitle() {
        SeoMetadata meta = seoService.forSearch("distributed");
        assertThat(meta.title()).contains("distributed");
        assertThat(meta.canonicalUrl()).endsWith("/search");
        assertThat(meta.noindex()).isTrue();
    }

    @Test
    void resolveFromPath_authorProfileAndSecondaryBlog() {
        SeoMetadata profile = seoService.resolveFromPath("/authors/" + author.getUsername());
        assertThat(profile.title()).contains(author.getName());

        SeoMetadata secondary = seoService.resolveFromPath("/" + author.getUsername() + "/" + secondaryBlog.getSlug());
        assertThat(secondary.title()).contains("Notes Blog");
    }

    @Test
    void resolveFromPath_blankPathReturnsHome() {
        assertThat(seoService.resolveFromPath("   ").title()).contains("Contraponto");
        assertThat(seoService.resolveFromPath(null).canonicalUrl()).endsWith("/");
    }

    @Test
    void resolveFromPath_manageAndAccountArePrivate() {
        assertThat(seoService.resolveFromPath("/manage").noindex()).isTrue();
        assertThat(seoService.resolveFromPath("/manage").title()).contains("Gerenciar");
        assertThat(seoService.resolveFromPath("/account").title()).contains("Conta");
        assertThat(seoService.resolveFromPath("/write").title()).contains("Escrever");
    }

    @Test
    void resolveFromPath_mapsPublishedPost() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("SEO Post Title")
                         .withSlug("seo-post")
                         .withContent("Body for SEO path test.")
                         .withDescription("Meta description for post")
                         .withPublished(true)
                         .persist();
        String path = "/" + author.getUsername() + "/post/seo-post";

        SeoMetadata meta = seoService.resolveFromPath(path);
        assertThat(meta.title()).contains("SEO Post Title");
        assertThat(meta.description()).contains("Meta description");
        assertThat(meta.noindex()).isFalse();
    }

    @Test
    void resolveFromPath_mapsReadingAndWritingHubs() {
        SeoMetadata reading = seoService.resolveFromPath("/reading/highlights");
        assertThat(reading.noindex()).isTrue();

        SeoMetadata writing = seoService.resolveFromPath("/writing");
        assertThat(writing.noindex()).isTrue();
        assertThat(writing.title()).contains("Escrita");
    }

    @Test
    void resolveFromPath_searchWithQueryParam() {
        SeoMetadata meta = seoService.resolveFromPath("/search?q=java");
        assertThat(meta.title()).contains("java");
        assertThat(meta.noindex()).isTrue();
    }

    @Test
    void resolveFromPath_tagAndSecondaryPost() {
        Given.transaction(() -> tagService.syncPostTags(
                                                        Given.post().withAuthor(author).withTitle("Tagged").withSlug("tagged-seo")
                                                             .withContent("Tagged body.").withPublished(true).persist(),
                                                        "[\"seo-tag\"]"));
        Tag tag = tagRepository.findBySlug("seo-tag").orElseThrow();
        SeoMetadata tagMeta = seoService.resolveFromPath("/tags/seo-tag");
        assertThat(tagMeta.title()).contains(tag.getName());

        Post secondaryPost = Given.post()
                                  .withAuthor(author)
                                  .withBlog(secondaryBlog)
                                  .withTitle("Secondary SEO")
                                  .withSlug("secondary-seo")
                                  .withContent("Secondary post body.")
                                  .withPublished(true)
                                  .persist();
        SeoMetadata postMeta = seoService.resolveFromPath(
                                                          "/" + author.getUsername() + "/" + secondaryBlog.getSlug() + "/post/secondary-seo");
        assertThat(postMeta.title()).contains("Secondary SEO");
        assertThat(postMeta.ogType()).isEqualTo(SeoOgType.ARTICLE);
    }

    @Test
    void resolveFromPath_unknownPathsFallBackToPrivate() {
        assertThat(seoService.resolveFromPath("/no-such-user").noindex()).isTrue();
        assertThat(seoService.resolveFromPath("/tags/missing-tag").noindex()).isTrue();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("seo-svc")
                      .withEmail("seo-svc@test.com")
                      .withName("SEO Service Author")
                      .withPassword("password123")
                      .persist();
        secondaryBlog = Given.blog()
                             .withUser(author)
                             .withName("Notes Blog")
                             .withSlug("notes")
                             .withDescription("Secondary blog for SEO tests.")
                             .persist();
        Given.post()
             .withAuthor(author)
             .withTitle("Public author post")
             .withSlug("public-author-post")
             .withContent("Published content for public author profile.")
             .withPublished(true)
             .persist();
    }
}
