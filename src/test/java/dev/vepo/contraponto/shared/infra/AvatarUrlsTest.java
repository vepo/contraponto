package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.user.User;

class AvatarUrlsTest {

    @Test
    void avatarUrlFallsBackToGeneratedUrl() {
        var user = new User();
        user.setName("Ada Lovelace");

        assertThat(AvatarUrls.avatarUrl(user)).startsWith("/components/avatar?name=");
    }

    @Test
    void avatarUrlPrefersProfilePicture() {
        var picture = new Image();
        picture.setUrl("/api/images/profile.png");
        var user = new User();
        user.setName("Ada Lovelace");
        user.setProfilePicture(picture);

        assertThat(AvatarUrls.avatarUrl(user)).isEqualTo("/api/images/profile.png");
    }

    @Test
    void generatedUrlEncodesDisplayName() {
        assertThat(AvatarUrls.generatedUrl("José Silva")).startsWith("/components/avatar?name=");
        assertThat(AvatarUrls.generatedUrl("José Silva")).contains("Jos");
    }

    @Test
    void generatedUrlReturnsEmptyForBlankName() {
        assertThat(AvatarUrls.generatedUrl("")).isEmpty();
        assertThat(AvatarUrls.generatedUrl(null)).isEmpty();
    }
}
