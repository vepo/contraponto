package dev.vepo.contraponto.activitypub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ActivityPubPostResolverTest {

    @Inject
    ActivityPubPostResolver resolver;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    @Test
    void ignoresDraftPost() {
        var user = Given.user()
                        .withUsername("resolverdraft")
                        .withEmail("resolverdraft@test.com")
                        .withPassword("password123")
                        .persist();
        var blog = new Blog(user);
        var post = new Post();
        post.setSlug("draft-only");
        post.setTitle("Draft");
        post.setBlog(blog);
        post.setPublished(false);
        Given.transaction(() -> {
            var em = Given.inject(jakarta.persistence.EntityManager.class);
            em.persist(blog);
            em.persist(post);
        });

        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        assertThat(resolver.resolvePublishedPostOwnedBy(objectUri, user)).isEmpty();
    }

    @Test
    void resolvesMainBlogPostByCanonicalPlatformUrl() {
        var user = Given.user()
                        .withUsername("resolvermain")
                        .withEmail("resolvermain@test.com")
                        .withPassword("password123")
                        .persist();
        var post = Given.post()
                        .withAuthor(user)
                        .withTitle("Resolver Main")
                        .withSlug("resolver-main")
                        .withContent("Body")
                        .persist();

        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var resolved = resolver.resolvePublishedPostOwnedBy(objectUri, user);

        assertThat(resolved).map(Post::getId).contains(post.getId());
    }

    @Test
    void resolvesPostByCreateActivityIdAlias() {
        var user = Given.user()
                        .withUsername("resolveralias")
                        .withEmail("resolveralias@test.com")
                        .withPassword("password123")
                        .persist();
        var post = Given.post()
                        .withAuthor(user)
                        .withTitle("Alias Post")
                        .withSlug("alias-post")
                        .withContent("Body")
                        .persist();

        var activityUri = ActivityPubPaths.activityId(user, subdomainConfig, "create", post.getId());
        var resolved = resolver.resolvePublishedPostOwnedBy(activityUri, user);

        assertThat(resolved).map(Post::getId).contains(post.getId());
    }

    @Test
    void resolvesSecondaryBlogPostByCanonicalPlatformUrl() {
        var user = Given.user()
                        .withUsername("resolversec")
                        .withEmail("resolversec@test.com")
                        .withPassword("password123")
                        .persist();
        var blog = Given.blog()
                        .withUser(user)
                        .withSlug("notes")
                        .withName("Notes")
                        .persist();
        var post = Given.post()
                        .withAuthor(user)
                        .withBlog(blog)
                        .withTitle("Resolver Secondary")
                        .withSlug("resolver-secondary")
                        .withContent("Body")
                        .persist();

        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var resolved = resolver.resolvePublishedPostOwnedBy(objectUri, user);

        assertThat(resolved).map(Post::getId).contains(post.getId());
        assertThat(objectUri).contains("/resolversec/notes/post/resolver-secondary");
    }
}
