package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class SiteIntegrationTest {

    @Test
    void configuredOnlyWhenUrlAndTokenPresent() {
        var url = SiteIntegration.resolveScriptUrl(Optional.of("https://visita.vepo.dev/visita.js"));
        var token = SiteIntegration.resolveDataToken(Optional.of("8f2e7dd9-69ee-411e-8acb-a2f5a4237fbc"));
        assertThat(url).contains("https://visita.vepo.dev/visita.js");
        assertThat(token).contains("8f2e7dd9-69ee-411e-8acb-a2f5a4237fbc");
    }

    @Test
    void notConfiguredWhenTokenMissing() {
        assertThat(SiteIntegration.resolveScriptUrl(Optional.of("https://visita.vepo.dev/visita.js"))).isPresent();
        assertThat(SiteIntegration.resolveDataToken(Optional.empty())).isEmpty();
    }

    @Test
    void resolveScriptOriginEmptyWhenUrlInvalid() {
        assertThat(SiteIntegration.resolveScriptOrigin(Optional.of("http://example.com/a.js"))).isEmpty();
        assertThat(SiteIntegration.resolveScriptOrigin(Optional.empty())).isEmpty();
    }

    @Test
    void resolveScriptOriginFromHttpsUrl() {
        assertThat(SiteIntegration.resolveScriptOrigin(Optional.of("https://visita.vepo.dev/visita.js")))
                                                                                                         .contains("https://visita.vepo.dev");
    }

    @Test
    void resolveScriptUrlAcceptsHttps() {
        assertThat(SiteIntegration.resolveScriptUrl(Optional.of("https://visita.vepo.dev/visita.js")))
                                                                                                      .contains("https://visita.vepo.dev/visita.js");
    }

    @Test
    void resolveScriptUrlRejectsHttpAndBlank() {
        assertThat(SiteIntegration.resolveScriptUrl(Optional.of("http://example.com/a.js"))).isEmpty();
        assertThat(SiteIntegration.resolveScriptUrl(Optional.of("   "))).isEmpty();
        assertThat(SiteIntegration.resolveScriptUrl(Optional.empty())).isEmpty();
    }
}
