package dev.vepo.contraponto.highlight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record HighlightAnchor(int start, int end, String prefix, String suffix) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static HighlightAnchor parse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Highlight anchor is required.");
        }
        try {
            return MAPPER.readValue(json, HighlightAnchor.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid highlight anchor JSON.", e);
        }
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize highlight anchor.", e);
        }
    }
}
