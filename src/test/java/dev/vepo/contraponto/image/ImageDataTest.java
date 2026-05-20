package dev.vepo.contraponto.image;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class ImageDataTest {

    @Test
    void equalityMatchesBytesContentTypeAndSize() {
        byte[] png = new byte[] { -119, 80 };
        ImageData a = new ImageData(png, "image/png", 2);
        ImageData clone = new ImageData(new byte[] { -119, 80 }, "image/png", 2);
        assertThat(a).isEqualTo(clone)
                     .isEqualTo(a);
        assertThat(a.equals(null)).isFalse();
        assertThat(a.contentType()).isEqualTo("image/png");
        assertThat(a.size()).isEqualTo(2);

        ImageData noisy = new ImageData(new byte[] { -119, -1 }, "image/png", 2);
        assertThat(a.equals(noisy)).isFalse();
        assertThat(a.hashCode()).isNotEqualTo(noisy.hashCode());
    }

    @Test
    void toStringMentionsEnvelopeMetadata() {
        ImageData blob = new ImageData(new byte[] { 10 }, "image/jpeg", 1);
        assertThat(blob.toString()).contains("jpeg", "size=1", "10");
    }
}
