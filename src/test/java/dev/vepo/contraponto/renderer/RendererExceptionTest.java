package dev.vepo.contraponto.renderer;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

@UnitTest
class RendererExceptionTest {

    @Test
    void exposesMessageWithoutCauseAndWithCause() {
        RendererException bare = new RendererException("boom");
        assertThat(bare).hasMessage("boom").hasNoCause();

        IOException cause = new IOException("underlying");
        RendererException wrapped = new RendererException("outer", cause);
        assertThat(wrapped).hasMessage("outer").hasCause(cause);
    }
}
