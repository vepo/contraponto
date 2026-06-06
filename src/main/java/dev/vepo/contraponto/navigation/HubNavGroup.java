package dev.vepo.contraponto.navigation;

import java.util.List;

public record HubNavGroup(String title, String titleI18nKey, List<HubSectionNav> sections) {

    public HubNavGroup(String title, List<HubSectionNav> sections) {
        this(title, null, sections);
    }
}
