package dev.vepo.contraponto.renderer;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class AsciiDoctorTest {

    @Test
    void getInstanceReturnsSingletonAndRendererProducesHtmlAroundBodyContent() {
        AsciiDoctor first = AsciiDoctor.getInstance();
        assertThat(first).isSameAs(AsciiDoctor.getInstance());

        String html = first.render("""
                                   = Fixture

                                   Paragraph text.""");
        assertThat(html).isNotBlank()
                        .containsIgnoringCase("paragraph");
    }
}
