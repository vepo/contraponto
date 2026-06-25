package dev.vepo.contraponto.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebAuthTest;
import dev.vepo.contraponto.user.User;

@WebAuthTest
class ProfileTest {

    private static final String TEST_USER_EMAIL = "profile@example.com";
    private static final String TEST_USER_PASSWORD = "profilePass123";
    private static final String TEST_USER_USERNAME = "profileuser";
    private static final String TEST_USER_NAME = "Profile Tester";

    private User testUser;

    @Test
    void authenticatedUserCanViewAccountSecurity(App app) {
        app.login(testUser)
           .accountSecurity()
           .assertEmailIs(TEST_USER_EMAIL);
    }

    @Test
    void authenticatedUserCanViewAuthorAppearance(App app) {
        app.login(testUser)
           .writingAppearance()
           .assertNameIs(TEST_USER_NAME);
    }

    @Test
    void changePasswordSuccessfully(App app) {
        app.login(testUser)
           .accountSecurity()
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .fillNewPassword("newPassword456")
           .fillConfirmPassword("newPassword456")
           .submit()
           .assertSuccessMessage("Account updated.")

           .logout()

           .access()
           .loginModal()
           .useLogin(TEST_USER_EMAIL)
           .usePassword("newPassword456")
           .submit()
           .assertModalWasClosed()

           .assertMenuIsDisplayed();
    }

    @Test
    void logoutFromSecurityRedirectsToHome(App app) {
        app.login(testUser)
           .accountSecurity()
           .waitForReady()
           .logout()
           .assertAccessButtonIsDisplayed();
    }

    @Test
    void passwordMismatchShowsError(App app) {
        app.login(testUser)
           .accountSecurity()
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
    void unauthenticatedUserCannotAccessAccountSecurity(App app) {
        app.access()
           .accountSecurity()
           .assertNotPresent();
    }

    @Test
    void updateDisplayNameSuccessfully(App app) {
        String newName = "Updated Name";

        app.login(testUser)
           .writingAppearance()
           .fillName(newName)
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .submit()
           .assertSuccessMessage("Appearance updated.")
           .refresh()
           .assertNameIs(newName);
    }

    @Test
    void updateProfileWithDuplicateEmailShowsError(App app) {
        Given.user()
             .withUsername("otheruser")
             .withEmail("other@example.com")
             .withPassword("otherPass")
             .withName("Other User")
             .persist();

        app.login(testUser)
           .accountSecurity()
           .fillEmail("other@example.com")
           .fillCurrentPassword(TEST_USER_PASSWORD)
           .submit()
           .assertErrorMessage("Email already registered");
    }

    @Test
    void updateProfileWithWrongCurrentPasswordShowsError(App app) {
        app.login(testUser)
           .accountSecurity()
           .fillEmail(TEST_USER_EMAIL)
           .fillCurrentPassword("wrongPassword")
           .submit()
           .assertErrorMessage("Current password is incorrect");
    }
}
