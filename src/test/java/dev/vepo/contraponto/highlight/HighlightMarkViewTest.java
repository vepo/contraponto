package dev.vepo.contraponto.highlight;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class HighlightMarkViewTest {

    @Test
    void notedIsTrueWhenPreviewPresent() {
        var withNote = new HighlightMarkView(1L, "passage", "{}", "hash", true, false, true, "My note");
        assertThat(withNote.noted()).isTrue();

        var withoutNote = new HighlightMarkView(2L, "passage", "{}", "hash", true, false, true, "");
        assertThat(withoutNote.noted()).isFalse();

        var blankNote = new HighlightMarkView(3L, "passage", "{}", "hash", true, false, true, "   ");
        assertThat(blankNote.noted()).isFalse();

        var nullNote = new HighlightMarkView(4L, "passage", "{}", "hash", true, false, true, null);
        assertThat(nullNote.noted()).isFalse();
    }
}
