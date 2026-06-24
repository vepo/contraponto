package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class BlogSubdomainConfigTest {

    @Test
    void isPlatformOnlyRootSegment_allowsAuthorAndWorkspacePathsOnSubdomain() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.isPlatformOnlyRootSegment("post")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("feed")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("components")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("manage")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("reading")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("administration")).isFalse();
        assertThat(config.isPlatformOnlyRootSegment("authors")).isTrue();
        assertThat(config.isPlatformOnlyRootSegment("explore")).isTrue();
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

        assertThat(config.platformUrl("/authors")).isEqualTo("https://blogs.commit-mestre.dev/authors");
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

    @Test
    void shouldSkipSubdomainRewrite_forWorkspaceHubs() {
        var config = new BlogSubdomainConfig(true, "commit-mestre.dev", "blogs.commit-mestre.dev", "https://blogs.commit-mestre.dev", false);

        assertThat(config.shouldSkipSubdomainRewrite("/administration")).isTrue();
        assertThat(config.shouldSkipSubdomainRewrite("/manage/dashboard")).isTrue();
        assertThat(config.shouldSkipSubdomainRewrite("/reading/saved")).isTrue();
    }
}
