package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class SiteBrandingTest {

    @Test
    void displayNamePreservesConfiguredCasing() {
        var branding = new SiteBranding("commit-mestre");
        assertThat(branding.displayName()).isEqualTo("commit-mestre");
        assertThat(branding.seoName()).isEqualTo("Commit Mestre");
    }

    @Test
    void seoNameFromDefaultProductName() {
        assertThat(SiteBranding.seoNameFrom("contraponto")).isEqualTo("Contraponto");
    }

    @Test
    void seoNameFromHyphenatedWhiteLabel() {
        assertThat(SiteBranding.seoNameFrom("commit-mestre")).isEqualTo("Commit Mestre");
    }
}
