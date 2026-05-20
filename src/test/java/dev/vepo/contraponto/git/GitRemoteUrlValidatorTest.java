package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class GitRemoteUrlValidatorTest {

    @Inject
    GitRemoteUrlValidator validator;

    @Test
    void acceptsNonGithubHttpsHost() {
        assertThat(validator.validate("https://gitlab.com/example/repo.git")).isEmpty();
    }

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
