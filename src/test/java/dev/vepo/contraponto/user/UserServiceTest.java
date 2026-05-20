package dev.vepo.contraponto.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class UserServiceTest {

    @Inject
    UserService userService;

    @Inject
    BlogRepository blogRepository;

    @Test
    void createUserDefaultsToUserRole() {
        User created = userService.createUser("brandnew", "Brand New", "brandnew@example.com", "Password123!", Set.of());

        assertThat(created.getRoles()).containsExactly(Role.USER);
        assertThat(blogRepository.findMainByOwnerId(created.getId())).isPresent();
    }

    @Test
    void rejectsBlankName() {
        assertThat(userService.validateNewUser("newuser", "  ", "new@example.com", "Password123!"))
                                                                                                   .hasValue("Name is required.");
    }

    @Test
    void rejectsDuplicateEmail() {
        Given.user()
             .withUsername("existinguser")
             .withEmail("shared@example.com")
             .withName("Existing")
             .withPassword("Password123!")
             .persist();

        assertThat(userService.validateNewUser("otheruser", "New User", "shared@example.com", "Password123!"))
                                                                                                              .hasValue("Email already registered.");
    }

    @Test
    void rejectsDuplicateUsername() {
        Given.user()
             .withUsername("takenuser")
             .withEmail("existing@example.com")
             .withName("Existing")
             .withPassword("Password123!")
             .persist();

        assertThat(userService.validateNewUser("takenuser", "New User", "other@example.com", "Password123!"))
                                                                                                             .hasValue("Username already taken.");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThat(userService.validateNewUser("newuser", "New User", "invalid-email", "Password123!"))
                                                                                                       .hasValue("Please enter a valid email address.");
    }

    @Test
    void rejectsShortPassword() {
        assertThat(userService.validateNewUser("newuser", "New User", "new@example.com", "short1"))
                                                                                                   .hasValue("Password must be at least 8 characters.");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }
}
