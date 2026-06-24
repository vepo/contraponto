package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;
import dev.vepo.contraponto.user.User;

@UnitTest
class BlogPublicUrlServiceTest {

    private BlogSubdomainConfig config;
    private BlogSubdomainContext context;
    private BlogPublicUrlService service;

    @Test
    void absoluteCanonical_usesAuthorSubdomain() {
        User owner = new User();
        owner.setUsername("vepo");
        Blog blog = new Blog(owner);
        var post = new dev.vepo.contraponto.post.Post();
        post.setBlog(blog);
        post.setSlug("hello-world");

        assertThat(service.absoluteCanonical(post)).isEqualTo("https://vepo.commit-mestre.dev/post/hello-world");
        assertThat(service.absoluteCanonical(blog)).isEqualTo("https://vepo.commit-mestre.dev/");
    }

    @Test
    void relativePath_onMatchingSubdomain_usesShortPath() {
        User owner = new User();
        owner.setUsername("vepo");
        Blog blog = new Blog(owner);
        var post = new dev.vepo.contraponto.post.Post();
        post.setBlog(blog);
        post.setSlug("hello-world");
        context.activate("vepo");

        assertThat(service.relativePath(post)).isEqualTo("/post/hello-world");
        assertThat(service.relativePath(blog)).isEqualTo("/");
    }

    @Test
    void relativePath_onPlatformHost_usesUsernamePrefix() {
        User owner = new User();
        owner.setUsername("vepo");
        Blog blog = new Blog(owner);
        var post = new dev.vepo.contraponto.post.Post();
        post.setBlog(blog);
        post.setSlug("hello-world");

        assertThat(service.relativePath(post)).isEqualTo("/vepo/post/hello-world");
    }

    @Test
    void secondaryBlogPaths_includeBlogSlugOnSubdomain() {
        User owner = new User();
        owner.setUsername("vepo");
        Blog blog = new Blog(owner, "notas", "Notas", "Secondary");
        var post = new dev.vepo.contraponto.post.Post();
        post.setBlog(blog);
        post.setSlug("draft");
        context.activate("vepo");

        assertThat(service.relativePath(post)).isEqualTo("/notas/post/draft");
        assertThat(service.absoluteCanonical(post)).isEqualTo("https://vepo.commit-mestre.dev/notas/post/draft");
    }

    @BeforeEach
    void setUp() {
        config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);
        context = new BlogSubdomainContext();
        service = new BlogPublicUrlService(config, context);
    }
}
