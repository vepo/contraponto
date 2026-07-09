package dev.vepo.contraponto.activitypub.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.blog.BlogSubdomainTestProfile;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
@TestProfile(BlogSubdomainTestProfile.class)
class ActivityPubPostResolverSubdomainTest {

    @Inject
    ActivityPubPostResolver resolver;

    @Inject
    BlogSubdomainConfig subdomainConfig;

    @Test
    void resolvesSecondaryBlogOnAuthorSubdomainPath() {
        var user = Given.user()
                        .withUsername("resolversubsec")
                        .withEmail("resolversubsec@test.com")
                        .withName("Resolver Sub Sec")
                        .withPassword("password123")
                        .persist();
        var blog = Given.blog()
                        .withUser(user)
                        .withSlug("notes")
                        .withName("Notes")
                        .withDescription("Secondary")
                        .persist();
        var post = Given.post()
                        .withAuthor(user)
                        .withBlog(blog)
                        .withTitle("Secondary Sub")
                        .withSlug("secondary-sub")
                        .withContent("Body")
                        .persist();

        var objectUri = ActivityPubPaths.postObjectId(post, subdomainConfig);
        assertThat(objectUri).isEqualTo("https://resolversubsec.localhost/notes/post/secondary-sub");
        assertThat(resolver.resolvePublishedPostOwnedBy(objectUri, user)).map(Post::getId).contains(post.getId());
    }

    @Test
    void resolvesSubdomainObjectIdAndLegacyPlatformObjectId() {
        var user = Given.user()
                        .withUsername("resolversub")
                        .withEmail("resolversub@test.com")
                        .withName("Resolver Sub")
                        .withPassword("password123")
                        .persist();
        var post = Given.post()
                        .withAuthor(user)
                        .withTitle("Subdomain Object")
                        .withSlug("subdomain-object")
                        .withContent("Body")
                        .persist();

        var actorHostObjectId = ActivityPubPaths.postObjectId(post, subdomainConfig);
        var legacyPlatformUri = subdomainConfig.platformUrl(PostPaths.extractUrl(post));

        assertThat(actorHostObjectId).isEqualTo("https://resolversub.localhost/post/subdomain-object");
        assertThat(legacyPlatformUri).isEqualTo("https://blogs.localhost/resolversub/post/subdomain-object");
        assertThat(actorHostObjectId).isNotEqualTo(legacyPlatformUri);

        assertThat(resolver.resolvePublishedPostOwnedBy(actorHostObjectId, user)).map(Post::getId).contains(post.getId());
        assertThat(resolver.resolvePublishedPostOwnedBy(legacyPlatformUri, user)).map(Post::getId).contains(post.getId());
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }
}
