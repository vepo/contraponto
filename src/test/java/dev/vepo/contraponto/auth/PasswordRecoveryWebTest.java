package dev.vepo.contraponto.auth;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;

@WebTest
class PasswordRecoveryWebTest {

    @Test
    void guestCanRequestPasswordRecovery(App app) {
        Given.user()
             .withUsername("recoverme")
             .withEmail("recover@example.com")
             .withName("Recover Me")
             .withPassword("oldPass1234")
             .persist();

        app.access()
           .passwordRecovery()
           .fillEmail("recover@example.com")
           .submit()
           .assertSuccessMessage("If an account exists for that email, we sent reset instructions.");
    }
}
