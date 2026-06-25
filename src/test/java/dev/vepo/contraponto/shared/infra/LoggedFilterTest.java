package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class LoggedFilterTest {

    @Test
    void safeReturnTo_allowsRelativePaths() {
        assertThat(LoggedFilter.safeReturnTo("/administration")).isEqualTo("/administration");
        assertThat(LoggedFilter.safeReturnTo("/administration/users?page=2")).isEqualTo("/administration/users?page=2");
    }

    @Test
    void safeReturnTo_rejectsOpenRedirects() {
        assertThat(LoggedFilter.safeReturnTo("https://evil.example")).isEqualTo("/");
        assertThat(LoggedFilter.safeReturnTo("//evil.example/path")).isEqualTo("/");
        assertThat(LoggedFilter.safeReturnTo(null)).isEqualTo("/");
    }
}
