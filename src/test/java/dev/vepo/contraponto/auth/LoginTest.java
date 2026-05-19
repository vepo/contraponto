package dev.vepo.contraponto.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.SignUpEndpoint;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;

@WebTest
class LoginTest {

    @Test
    void invalidCredentialsShowsErrorMessage(App app) {
        app.access()
           .loginModal()
           // Fill with valid format but wrong credentials
           .useLogin("wrong@example.com")
           .usePassword("wrongPassword")
           .assertSubmitEnabled()
           .submit()
           // Expect an error message inside #authError (or a global error div)
           .assertErrorMessage("Invalid username/email or password.")
           // Modal should still be open
           .assertModalIsOpen()
           // Submit button should remain enabled (user can retry)
           .assertSubmitEnabled();
    }

    @Test
    void loginModalValidationAndSuccess(App app) {
        app.access()
           // Open login modal
           .loginModal()
           // Initially disabled
           .assertSubmitDisabled()
           // Test empty fields. It should not be pristine
           .useLogin("abc")
           .useLogin("")
           .usePassword("validPassword123")
           .assertSubmitDisabled()
           // Test invalid login (non-existent)
           .useLogin("nonexistent")
           .assertSubmitEnabled()
           .submit()
           .assertErrorMessage("Invalid username/email")
           // Successful login with email
           .useLogin("test@example.com")
           .usePassword("validPassword123")
           .assertSubmitEnabled()
           .submit()
           // Modal closes, user menu appears and opens on click
           .assertModalWasClosed()
           .assertMenuIsDisplayed()
           .openUserMenu()
           // - Optionally check that the session cookie is set
           .assertCookieIsPresent(SignUpEndpoint.SESSION_COOKIE_NAME);
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        Given.user()
             .withUsername("validuser")
             .withEmail("test@example.com")
             .withPassword("validPassword123")
             .withName("Valid User")
             .persist();
    }
}