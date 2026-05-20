package dev.vepo.contraponto.shared.infra;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class AvatarSvgRendererTest {

    @Test
    void renderCapsInitialsAtTwoCharacters() {
        var svg = AvatarSvgRenderer.render("ABCD");

        assertThat(svg).contains(">AB</text>");
        assertThat(svg).doesNotContain(">ABCD</text>");
    }

    @Test
    void renderEscapesXmlInInitials() {
        var svg = AvatarSvgRenderer.render("A<");

        assertThat(svg).contains(">A&lt;</text>");
        assertThat(svg).doesNotContain(">A<</text>");
    }

    @Test
    void renderIncludesBrandColorsAndInitials() {
        var svg = AvatarSvgRenderer.render("AL");

        assertThat(svg).contains("#1a8917");
        assertThat(svg).contains("#ffffff");
        assertThat(svg).contains(">AL</text>");
    }
}
