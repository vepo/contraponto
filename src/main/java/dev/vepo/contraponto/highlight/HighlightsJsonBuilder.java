package dev.vepo.contraponto.highlight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HighlightsJsonBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private record HighlightsPayload(java.util.List<HighlightMarkView> marks,
                                     java.util.List<OfficialHighlightView> official) {}

    public String build(HighlightsSectionView section) {
        try {
            return MAPPER.writeValueAsString(new HighlightsPayload(section.marks(),
                                                                   section.officialHighlights()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize highlights.", e);
        }
    }
}
