package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageAssetsFilterTest {

    @Test
    void resolvesManageForOperationsHubs() {
        assertThat(PageAssetsFilter.resolveProfile("/manage/dashboard")).isEqualTo(PageAssets.MANAGE);
        assertThat(PageAssetsFilter.resolveProfile("/writing/blogs")).isEqualTo(PageAssets.MANAGE);
        assertThat(PageAssetsFilter.resolveProfile("/blogs/1/edit")).isEqualTo(PageAssets.MANAGE);
    }

    @Test
    void resolvesPostReadForPublishedPosts() {
        assertThat(PageAssetsFilter.resolveProfile("/alice/post/my-slug")).isEqualTo(PageAssets.POST_READ);
        assertThat(PageAssetsFilter.resolveProfile("/alice/notes/post/my-slug")).isEqualTo(PageAssets.POST_READ);
    }

    @Test
    void resolvesPublicReadForHomeAndExplore() {
        assertThat(PageAssetsFilter.resolveProfile("/")).isEqualTo(PageAssets.PUBLIC_READ);
        assertThat(PageAssetsFilter.resolveProfile("explore/blogs")).isEqualTo(PageAssets.PUBLIC_READ);
        assertThat(PageAssetsFilter.resolveProfile("/authors")).isEqualTo(PageAssets.PUBLIC_READ);
    }

    @Test
    void resolvesWriteForWriteRoutes() {
        assertThat(PageAssetsFilter.resolveProfile("/write")).isEqualTo(PageAssets.WRITE);
        assertThat(PageAssetsFilter.resolveProfile("/write/draft/42")).isEqualTo(PageAssets.WRITE);
    }
}
