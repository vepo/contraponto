package dev.vepo.contraponto.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class ProfileTest {

    private static final String TEST_USER_EMAIL = "profile@example.com";
    private static final String TEST_USER_PASSWORD = "profilePass123";
    private static final String TEST_USER_USERNAME = "profileuser";
    private static final String TEST_USER_NAME = "Profile Tester";

    private User testUser;

    @Test
    void authenticatedUserCanViewProfile(App app) {
        app.login(testUser)
           .profile()
           .assertNameIs(TEST_USER_NAME)
           .assertEmailIs(TEST_USER_EMAIL);
    }

    @Test
    void changePasswordSuccessfully(App app) {
        // Login and go to profile
        app.login(testUser)
           .profile()
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .fillNewPassword("newPassword456")
           .fillConfirmPassword("newPassword456")
           .submit()
           .assertSuccessMessage("Profile updated.")

           // Logout
           .logout()

           // Try to login with new password (using UI modal to verify)
           .access()
           .loginModal()
           .useLogin(TEST_USER_EMAIL)
           .usePassword("newPassword456")
           .submit()
           .assertModalWasClosed()

           // Verify user menu appears (logged in)
           .assertMenuIsDisplayed();
    }

    @Test
    void logoutFromProfileRedirectsToHome(App app) {
        app.login(testUser)
           .profile()
           .waitForReady()
           .logout()
           // Wait for login button to appear
           .assertAccessButtonIsDisplayed();
    }

    @Test
    void passwordMismatchShowsError(App app) {
        app.login(testUser)
           .profile()
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .fillNewPassword("newPassword456")
           .fillConfirmPassword("differentPassword")
           .submit()
           .assertErrorMessage("Passwords do not match");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername(TEST_USER_USERNAME)
                        .withEmail(TEST_USER_EMAIL)
                        .withPassword(TEST_USER_PASSWORD)
                        .withName(TEST_USER_NAME)
                        .persist();
    }

    @Test
    void unauthenticatedUserCannotAccessProfile(App app) {
        app.access()
           .profile()
           .assertNotPresent();
    }

    @Test
    void updateProfileSuccessfully(App app) {
        String newName = "Updated Name";

        app.login(testUser)
           .profile()
           .fillName(newName)
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .submit()
           .assertSuccessMessage("Profile updated.")
           .refresh()
           .assertNameIs(newName)
           .assertEmailIs(TEST_USER_EMAIL);
    }

    @Test
    void updateProfileWithDuplicateEmailShowsError(App app) {
        // Create another user with a different email
        Given.user()
             .withUsername("otheruser")
             .withEmail("other@example.com")
             .withPassword("otherPass")
             .withName("Other User")
             .persist();

        app.login(testUser)
           .profile()
           .fillEmail("other@example.com")
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .submit()
           .assertErrorMessage("Email already registered");
    }

    @Test
    void updateProfileWithWrongCurrentPasswordShowsError(App app) {
        app.login(testUser)
           .profile()
           .fillName("Any Name")
           .fillCurrentPassword("wrongPassword")
           .submit()
           .assertErrorMessage("Current password is incorrect");
    }
}