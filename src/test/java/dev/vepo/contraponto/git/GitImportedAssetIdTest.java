package dev.vepo.contraponto.git;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class GitImportedAssetIdTest {

    private static final String CANONICAL = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void differentExtensionsProduceDifferentIds() {
        String base = "shared-basename";
        assertThat(GitImportedAssetId.normalize(base, ".png"))
                                                              .isNotEqualTo(GitImportedAssetId.normalize(base, ".gif"));
    }

    @Test
    void mapsLongJekyllFilenameToDeterministicUuid() {
        String longName = "kafka-distributed-systems-architecture-diagram-overview";
        String first = GitImportedAssetId.normalize(longName, ".png");
        String second = GitImportedAssetId.normalize(longName, ".png");

        assertThat(first).hasSize(36).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void preservesCanonicalUuid() {
        assertThat(GitImportedAssetId.normalize(CANONICAL, ".png")).isEqualTo(CANONICAL);
    }
}
