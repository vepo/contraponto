package dev.vepo.contraponto.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;

@WebAuthTest
class SignupTest {

    @Test
    void duplicateEmailShowsErrorMessage(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           .useUsername("duplicated")
           .useName("Duplicate Tester")
           // Fill with existing email
           .useEmail("existing@example.com")
           .usePassword("anyPassword123")
           .assertSubmitEnabled()
           .submit()
           // Expect error message inside #authError or .response.error
           .assertErrorMessage("Email already registered")
           // Modal should still be open, submit button remains enabled
           .assertModalIsOpen()
           .assertSubmitEnabled();
    }

    @Test
    void invalidEmailFormatShowsErrorWithoutSubmitting(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           // load validation scripts
           .waitForReady()
           .useEmail("not-an-email")
           .usePassword("anyPassword123")
           // wait validation ends
           .waitForReady()
           .assertFieldError("Please enter a valid email address.")
           .assertSubmitDisabled()
           // Fix email
           .useEmail("good@example.com")
           // Error should disappear
           .assertNoFieldErrorMessage()
           // Now button may still be disabled because other fields are emtpy
           .assertSubmitDisabled();
    }

    @Test
    void missingNameShowsErrorMessage(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           .waitForReady()
           // 1. Fill email and password (they will become non‑pristine later)
           .usePassword("password123")
           // 2. Make the name field non‑pristine:
           // - Focus, type something (changes value → hasChanged = true)
           // - Clear it (value becomes empty)
           // - Blur (sets pristine = false)
           .useName("temp")
           // Blur by clicking on email field (or any other)
           .useEmail("any-email@example.com")
           .useName("")
           // Now name field is empty and non‑pristine → error should appear
           .waitForReady()
           .assertFieldError("Name is required.")
           .assertSubmitDisabled()
           // 3. Fill name correctly → error disappears, button enabled
           .useName("Valid Name")
           .useUsername("validauser")
           .waitForReady()
           .assertNoFieldErrorMessage()
           .assertSubmitEnabled();
    }

    @Test
    void reservedUsernameShowsError(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           .useUsername("pages")
           .useName("Reserved User")
           .useEmail("reserved@example.com")
           .usePassword("password12345")
           .assertSubmitEnabled()
           .submit()
           .assertErrorMessage("This username is reserved and cannot be used.")
           .assertModalIsOpen();
    }

    @BeforeEach
    void setup() {
        // Create an existing user to test duplicate email scenario
        Given.cleanup();
        Given.user()
             .withUsername("existing")
             .withEmail("existing@example.com")
             .withPassword("password123")
             .withName("Existing User")
             .persist();
    }

    @Test
    void signupModalValidationAndSuccess(App app) {
        app.access()
           .loginModal()
           .switchToSignup()
           .waitForReady()
           .assertSubmitDisabled()
           // Username validation (too short)
           .useUsername("ab")
           .usePassword("anyPassword123")
           .waitForReady()
           .assertFieldError("Username must be at least 3 characters.")
           .assertSubmitDisabled()
           // Fix username
           .useUsername("newuser")
           // Now fill other fields
           .useEmail("new@example.com")
           .useName("New User")
           .assertSubmitEnabled()
           // Submit
           .submit()
           .assertModalWasClosed()
           .assertToastSuccess("Check your email to activate your account.")
           .assertAccessButtonIsDisplayed();
    }
}