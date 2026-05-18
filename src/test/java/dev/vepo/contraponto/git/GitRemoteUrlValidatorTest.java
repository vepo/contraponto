package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class GitRemoteUrlValidatorTest {

    @Inject
    GitRemoteUrlValidator validator;

    @Test
    void acceptsPublicHttpsUrl() {
        assertThat(validator.validate("https://github.com/example/repo.git")).isEmpty();
    }

    @Test
    void allowsBlank() {
        assertThat(validator.validate("")).isEmpty();
        assertThat(validator.validate(null)).isEmpty();
    }

    @Test
    void rejectsLocalhost() {
        assertThat(validator.validate("https://localhost/repo.git")).isPresent();
    }

    @Test
    void rejectsNonHttpsScheme() {
        assertThat(validator.validate("http://github.com/example/repo.git")).isPresent();
    }
}
