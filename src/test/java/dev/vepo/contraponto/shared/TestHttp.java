package dev.vepo.contraponto.shared;

import static io.restassured.RestAssured.given;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.shared.i18n.LocalePreference;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.shared.security.CsrfTokenService;
import dev.vepo.contraponto.user.User;
import io.restassured.specification.RequestSpecification;

/**
 * RestAssured helpers for integration tests. Mutating {@code /forms/*} and
 * {@code /api/images} requests require a session cookie plus matching CSRF
 * cookie and header (see
 * {@link dev.vepo.contraponto.shared.security.CsrfFilter}).
 */
public final class TestHttp {

    public static RequestSpecification authenticated(User user) {
        var sessionId = Given.inject(LoggedUserProvider.class).login(user).getSessionId();
        var bootstrap = given().redirects()
                               .follow(true)
                               .cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
                               .when()
                               .get("/");
        String csrf = bootstrap.getCookie(CsrfTokenService.COOKIE_NAME);
        if (csrf == null || csrf.isBlank()) {
            throw new IllegalStateException("CSRF cookie missing after bootstrap GET /");
        }
        return given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
                      .cookie(CsrfTokenService.COOKIE_NAME, csrf)
                      .cookie(LocalePreference.COOKIE_NAME, "en")
                      .header(CsrfTokenService.HEADER_NAME, csrf);
    }

    public static RequestSpecification session(User user) {
        var sessionId = Given.inject(LoggedUserProvider.class).login(user).getSessionId();
        return given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId);
    }

    private TestHttp() {}
}
