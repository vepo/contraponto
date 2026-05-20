package dev.vepo.contraponto.shared.infra;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class DisplayNameInitialsTest {

    @Test
    void fromBlankReturnsEmpty() {
        assertThat(DisplayNameInitials.from("   ")).isEmpty();
        assertThat(DisplayNameInitials.from(null)).isEmpty();
    }

    @Test
    void fromFullNameUsesFirstAndLastInitials() {
        assertThat(DisplayNameInitials.from("Ada Lovelace")).isEqualTo("AL");
    }

    @Test
    void fromSingleNameUsesFirstLetter() {
        assertThat(DisplayNameInitials.from("José")).isEqualTo("J");
    }
}
