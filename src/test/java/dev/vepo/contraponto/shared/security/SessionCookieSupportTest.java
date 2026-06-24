package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class SessionCookieSupportTest {

    @Test
    void buildSessionCookieAddsDomainWhenConfigured() {
        var support = new SessionCookieSupport(true, 86400, ".example.test");

        var cookie = support.buildSessionCookie("abc-123");

        assertThat(cookie).contains("Domain=.example.test");
        assertThat(cookie).endsWith("; Secure");
    }

    @Test
    void buildSessionCookieAddsSecureWhenEnabled() {
        var support = new SessionCookieSupport(true, 2592000, (String) null);

        var cookie = support.buildSessionCookie("abc-123");

        assertThat(cookie).startsWith(SessionConstants.SESSION_COOKIE_NAME + "=abc-123");
        assertThat(cookie).contains("Max-Age=2592000");
        assertThat(cookie).contains("SameSite=Lax");
        assertThat(cookie).endsWith("; Secure");
    }

    @Test
    void buildSessionCookieUsesLaxSameSiteAndConfiguredTtl() {
        var support = new SessionCookieSupport(false, 86400, (String) null);

        var cookie = support.buildSessionCookie("abc-123");

        assertThat(cookie).isEqualTo("__session=abc-123; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax");
    }
}
