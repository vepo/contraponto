package dev.vepo.contraponto.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ContentImageMarkerServiceTest {

    @Inject
    ContentImageMarkerService markerService;

    @Test
    void extractsUuidsFromMarkersAndUrls() {
        String content = """
                         <!-- contraponto:image uuid="550e8400-e29b-41d4-a716-446655440000" -->
                         ![a](/api/images/550e8400-e29b-41d4-a716-446655440000.png)
                         image::/api/images/6ba7b810-9dad-11d1-80b4-00c04fd430c8.jpg[]
                         """;
        assertThat(markerService.extractImageUuids(content)).containsExactlyInAnyOrder("550e8400-e29b-41d4-a716-446655440000",
                                                                                       "6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    }

    @Test
    void injectsMarkersOnStore() {
        String editor = "![pic](/api/images/550e8400-e29b-41d4-a716-446655440000.png)";
        String stored = markerService.toStoredContent(editor);
        assertThat(stored).contains("<!-- contraponto:image uuid=\"550e8400-e29b-41d4-a716-446655440000\" -->");
    }

    @Test
    void stripMarkersForExportMatchesEditorContent() {
        String stored = """
                        <!-- contraponto:image uuid="550e8400-e29b-41d4-a716-446655440000" -->
                        ![pic](/api/images/550e8400-e29b-41d4-a716-446655440000.png)
                        """;
        assertThat(markerService.stripMarkersForExport(stored)).isEqualTo(markerService.toEditorContent(stored));
    }

    @Test
    void stripsMarkersForEditor() {
        String stored = """
                        <!-- contraponto:image uuid="550e8400-e29b-41d4-a716-446655440000" -->
                        ![pic](/api/images/550e8400-e29b-41d4-a716-446655440000.png)
                        """;
        assertThat(markerService.toEditorContent(stored)).doesNotContain("contraponto:image");
        assertThat(markerService.toEditorContent(stored)).contains("/api/images/");
    }
}
