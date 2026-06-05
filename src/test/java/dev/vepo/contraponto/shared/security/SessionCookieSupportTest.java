package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class SessionCookieSupportTest {

    @Test
    void buildSessionCookieAddsSecureWhenEnabled() {
        var support = new SessionCookieSupport(true, 2592000);

        var cookie = support.buildSessionCookie("abc-123");

        assertThat(cookie).startsWith(LoginEndpoint.SESSION_COOKIE_NAME + "=abc-123");
        assertThat(cookie).contains("Max-Age=2592000");
        assertThat(cookie).contains("SameSite=Lax");
        assertThat(cookie).endsWith("; Secure");
    }

    @Test
    void buildSessionCookieUsesLaxSameSiteAndConfiguredTtl() {
        var support = new SessionCookieSupport(false, 86400);

        var cookie = support.buildSessionCookie("abc-123");

        assertThat(cookie).isEqualTo("__session=abc-123; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax");
    }
}
