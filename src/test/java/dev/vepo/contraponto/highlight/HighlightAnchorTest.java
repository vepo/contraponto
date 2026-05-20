package dev.vepo.contraponto.highlight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HighlightAnchorTest {

    @Test
    void parse_rejects_invalid_json() {
        assertThrows(IllegalArgumentException.class, () -> HighlightAnchor.parse("{not-json"));
    }

    @Test
    void parse_rejects_null_or_blank() {
        assertThrows(IllegalArgumentException.class, () -> HighlightAnchor.parse(null));
        assertThrows(IllegalArgumentException.class, () -> HighlightAnchor.parse("  "));
    }

    @Test
    void parse_round_trips_json() {
        HighlightAnchor anchor = HighlightAnchor.parse("{\"start\":1,\"end\":10,\"prefix\":\"a\",\"suffix\":\"b\"}");
        assertThat(anchor.start()).isEqualTo(1);
        assertThat(anchor.end()).isEqualTo(10);
        assertThat(anchor.prefix()).isEqualTo("a");
        assertThat(anchor.suffix()).isEqualTo("b");
        assertThat(anchor.toJson()).contains("\"start\":1");
    }
}
