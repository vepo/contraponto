package dev.vepo.contraponto.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
