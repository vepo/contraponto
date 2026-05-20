package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.user.User;

@UnitTest
class BlogBannerServiceTest {

    private static Image image(String uuid) {
        var image = new Image();
        image.setUuid(uuid);
        image.setUrl("/api/images/" + uuid);
        return image;
    }

    private final BlogBannerService blogBannerService = new BlogBannerService(null, null);

    @Test
    void applyBannerFromFormClearsWhenBlank() {
        var owner = new User();
        owner.setId(1L);
        var blog = new Blog();
        blog.setOwner(owner);
        blog.setBanner(image("existing"));

        blogBannerService.applyBannerFromForm(blog, "");

        assertThat(blog.getBanner()).isNull();
    }

    @Test
    void applyDefaultBannerOnCreateCopiesOwnerDefault() {
        var owner = new User();
        var defaultBanner = image("default-banner");
        owner.setDefaultBlogBanner(defaultBanner);

        var blog = new Blog();
        blog.setOwner(owner);

        blogBannerService.applyDefaultBannerOnCreate(blog);

        assertThat(blog.getBanner()).isEqualTo(defaultBanner);
    }

    @Test
    void resolveEffectiveBannerFallsBackToUserDefault() {
        var owner = new User();
        var defaultBanner = image("default-banner");
        owner.setDefaultBlogBanner(defaultBanner);

        var blog = new Blog();
        blog.setOwner(owner);

        assertThat(blogBannerService.resolveEffectiveBanner(blog)).contains(defaultBanner);
    }

    @Test
    void resolveEffectiveBannerPrefersBlogBanner() {
        var owner = new User();
        var blogBanner = image("blog-banner");
        var defaultBanner = image("default-banner");
        owner.setDefaultBlogBanner(defaultBanner);

        var blog = new Blog();
        blog.setOwner(owner);
        blog.setBanner(blogBanner);

        assertThat(blogBannerService.resolveEffectiveBanner(blog)).contains(blogBanner);
    }
}
