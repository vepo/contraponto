package dev.vepo.contraponto.image;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class ImageTest {

    @Test
    void equalsRejectsNullOrDifferentId() {
        var image = new Image();
        image.setId(2L);
        assertThat(image.equals(null)).isFalse();
        assertThat(image).isNotEqualTo(new Image());
    }

    @Test
    void equalsUsesPersistedId() {
        var left = new Image();
        left.setId(1L);
        var right = new Image();
        right.setId(1L);
        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }

    @Test
    void toStringIncludesIdentifiers() {
        var image = new Image();
        image.setId(9L);
        image.setUuid("uuid-1");
        image.setFilename("pic.png");
        image.setUrl("/api/images/uuid-1.png");
        assertThat(image.toString()).contains("uuid-1", "pic.png", "/api/images/uuid-1.png");
    }
}
