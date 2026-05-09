package dev.vepo.contraponto.custompage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record Links(Map<PagePlacement, List<Section>> sections) {

    public List<Section> sections(String placement) {
        return sections(PagePlacement.valueOf(placement));
    }

    public List<Section> sections(PagePlacement placement) {
        return sections.getOrDefault(placement, Collections.emptyList());
    }
}
