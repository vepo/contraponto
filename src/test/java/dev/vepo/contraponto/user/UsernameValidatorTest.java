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
    void rejectsReservedUsername() {
        assertThat(usernameValidator.validate("pages")).hasValue("This username is reserved and cannot be used.");
    }
}
