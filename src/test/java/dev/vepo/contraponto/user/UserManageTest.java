package dev.vepo.contraponto.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;

@WebTest
class UserManageTest {

    private static final String ADMIN_EMAIL = "useradmin@example.com";
    private static final String ADMIN_PASSWORD = "userAdminPass123";
    private static final String ADMIN_USERNAME = "useradmin";

    private dev.vepo.contraponto.user.User adminUser;

    @Test
    void adminCanCreateUser(App app) {
        app.login(adminUser)
           .newUser()
           .assertTitle("New User")
           .fillUsername("newmember")
           .fillName("New Member")
           .fillEmail("newmember@example.com")
           .fillPassword("memberPass123")
           .submit()
           .assertUrl("/administration/users")
           .assertToastSuccess("User created successfully.")
           .assertUserListed("New Member");
    }

    @Test
    void adminCanDeactivateUser(App app) {
        Given.user()
             .withUsername("deactivateme")
             .withEmail("deactivate@example.com")
             .withName("Deactivate Me")
             .withPassword("memberPass123")
             .persist();

        app.login(adminUser)
           .users()
           .clickEdit("Deactivate Me")
           .setActive(false)
           .submit()
           .assertUrl("/administration/users");

        app.clearAuth()
           .loginModal()
           .useLogin("deactivate@example.com")
           .usePassword("memberPass123")
           .submit()
           .assertErrorMessage("Invalid username/email or password.");
    }

    @Test
    void adminCanResetPassword(App app) {
        Given.user()
             .withUsername("resetme")
             .withEmail("reset@example.com")
             .withName("Reset Me")
             .withPassword("oldPass1234")
             .persist();

        app.login(adminUser)
           .users()
           .clickEdit("Reset Me")
           .fillNewPassword("newPass45678")
           .submit()
           .assertUrl("/administration/users");
    }

    @Test
    void adminCanUpdateUserEmail(App app) {
        Given.user()
             .withUsername("emailuser")
             .withEmail("old@example.com")
             .withName("Email User")
             .withPassword("memberPass123")
             .persist();

        app.login(adminUser)
           .users()
           .clickEdit("Email User")
           .fillEmail("updated@example.com")
           .submit()
           .assertUrl("/administration/users")
           .assertUserListed("Email User");
    }

    @Test
    void nonAdminCannotAccessUsers(App app) {
        var regular = Given.user()
                           .withUsername("regularonly")
                           .withEmail("regular@example.com")
                           .withName("Regular Only")
                           .withPassword("regularPass123")
                           .persist();

        app.login(regular)
           .users()
           .assertManagePageNotLoaded();
    }

    @Test
    void reservedUsernameRejectedOnCreate(App app) {
        app.login(adminUser)
           .newUser()
           .fillUsername("pages")
           .fillName("Bad Name")
           .fillEmail("bad@example.com")
           .fillPassword("badPass1234")
           .submit()
           .assertTitle("New User");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        adminUser = Given.user()
                         .withUsername(ADMIN_USERNAME)
                         .withEmail(ADMIN_EMAIL)
                         .withPassword(ADMIN_PASSWORD)
                         .withName("User Admin")
                         .withRoles(Role.USER_ADMINISTRATOR, Role.USER)
                         .persist();
    }
}
