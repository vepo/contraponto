package dev.vepo.contraponto.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UsernameValidatorTest {

    @Inject
    UsernameValidator usernameValidator;

    @Test
    void acceptsValidUsername() {
        assertThat(usernameValidator.validate("valid-user_1")).isEmpty();
    }

    @Test
    void rejectsInvalidCharacters() {
        assertThat(usernameValidator.validate("user name"))
                                                           .hasValue("Username must start with a letter or number and contain only letters, numbers, hyphens and underscores.");
        assertThat(usernameValidator.validate("-starts-with-dash"))
                                                                   .hasValue("Username must start with a letter or number and contain only letters, numbers, hyphens and underscores.");
    }

    @Test
    void rejectsNullOrBlankUsername() {
        assertThat(usernameValidator.validate(null)).hasValue("Username is required.");
        assertThat(usernameValidator.validate("")).hasValue("Username is required.");
        assertThat(usernameValidator.validate("   ")).hasValue("Username is required.");
    }

    @Test
    void rejectsReservedRouteUsername() {
        assertThat(usernameValidator.validate("write")).hasValue("This username is reserved and cannot be used.");
    }

    @Test
    void rejectsReservedUsername() {
        assertThat(usernameValidator.validate("pages")).hasValue("This username is reserved and cannot be used.");
    }

    @Test
    void rejectsTooLongUsername() {
        assertThat(usernameValidator.validate("a".repeat(21)))
                                                              .hasValue("Username must be between 3 and 20 characters.");
    }

    @Test
    void rejectsTooShortUsername() {
        assertThat(usernameValidator.validate("ab")).hasValue("Username must be between 3 and 20 characters.");
    }

    @Test
    void rejects_feed_username() {
        assertThat(usernameValidator.validate("feed")).hasValue("This username is reserved and cannot be used.");
    }
}
