package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class SecurityHeadersFilterTest {

    @Test
    void contentSecurityPolicyAllowsConfiguredSiteIntegrationOrigin() {
        var csp = SecurityHeadersFilter.contentSecurityPolicy(Optional.of("https://visita.vepo.dev"));

        assertThat(csp).contains("script-src 'self' 'unsafe-inline' https://platform.twitter.com https://visita.vepo.dev;");
        assertThat(csp).contains("connect-src 'self' https://visita.vepo.dev;");
        assertThat(csp).contains("frame-src https://platform.twitter.com https://www.youtube.com https://www.youtube-nocookie.com;");
    }

    @Test
    void contentSecurityPolicyWithoutSiteIntegration() {
        var csp = SecurityHeadersFilter.contentSecurityPolicy(Optional.empty());

        assertThat(csp).contains("script-src 'self' 'unsafe-inline' https://platform.twitter.com;");
        assertThat(csp).contains("connect-src 'self';");
        assertThat(csp).contains("frame-src https://platform.twitter.com https://www.youtube.com https://www.youtube-nocookie.com;");
        assertThat(csp).doesNotContain("visita.vepo.dev");
    }
}
