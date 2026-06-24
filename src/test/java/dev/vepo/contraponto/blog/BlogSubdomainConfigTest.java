package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class BlogSubdomainConfigTest {

    @Test
    void isPlatformOnlyRootSegment_allowsAuthorPostPathsOnSubdomain() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.isPlatformOnlyRootSegment("post")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("feed")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("components")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("manage")).isTrue();
        assertThat(config.isPlatformOnlyRootSegment("reading")).isTrue();
    }

    @Test
    void parseUserSubdomain_recognizesAuthorHost() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.parseUserSubdomain("vepo.commit-mestre.dev")).contains("vepo");
        assertThat(config.parseUserSubdomain("blogs.commit-mestre.dev")).isEmpty();
        assertThat(config.parseUserSubdomain("localhost")).isEmpty();
    }

    @Test
    void platformUrl_buildsHttpsRedirectTarget() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.platformUrl("/manage")).isEqualTo("https://blogs.commit-mestre.dev/manage");
    }

    @Test
    void shouldSkipSubdomainRewrite_forAuthModal() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.shouldSkipSubdomainRewrite("/auth/modal")).isTrue();
    }

    @Test
    void shouldSkipSubdomainRewrite_forFavicon() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.shouldSkipSubdomainRewrite("/favicon.svg")).isTrue();
        assertThat(config.shouldSkipSubdomainRewrite("/favicon.ico")).isTrue();
    }

    @Test
    void shouldSkipSubdomainRewrite_forStaticAssets() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.shouldSkipSubdomainRewrite("/js/main.js")).isTrue();
        assertThat(config.shouldSkipSubdomainRewrite("/post/slug")).isFalse();
    }
}
