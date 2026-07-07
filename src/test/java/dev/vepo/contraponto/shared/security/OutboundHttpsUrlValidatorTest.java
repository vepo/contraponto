package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class OutboundHttpsUrlValidatorTest {

    @Inject
    OutboundHttpsUrlValidator validator;

    @Test
    void acceptsPublicHttpsUrl() {
        assertThat(validator.validateHttpsUrl("https://mastodon.social/users/alice")).isEmpty();
    }

    @Test
    void allowsTestConfiguredLocalhost() {
        assertThat(validator.validateHttpsUrl("https://127.0.0.1/users/alice")).isEmpty();
    }

    @Test
    void rejectsBlockedPrivateHost() {
        assertThat(validator.validateHttpsUrl("https://10.0.0.1/users/alice")).isPresent();
    }

    @Test
    void rejectsNonHttpsScheme() {
        assertThat(validator.validateHttpsUrl("http://mastodon.social/users/alice")).isPresent();
    }
}
