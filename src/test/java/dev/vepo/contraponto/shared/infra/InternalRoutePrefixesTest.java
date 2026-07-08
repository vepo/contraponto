package dev.vepo.contraponto.shared.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class InternalRoutePrefixesTest {

    @Test
    void constants_matchOf() {
        assertThat(InternalRoutePrefixes.CUSTOM_PAGE).isEqualTo(InternalRoutePrefixes.of("custom_page"));
        assertThat(InternalRoutePrefixes.ACTIVITY_PUB).isEqualTo(InternalRoutePrefixes.of("activity_pub"));
    }

    @Test
    void of_buildsDoubleUnderscoreSnakeCasePrefix() {
        assertThat(InternalRoutePrefixes.of("custom_page")).isEqualTo("/__custom_page__");
        assertThat(InternalRoutePrefixes.of("activity_pub")).isEqualTo("/__activity_pub__");
    }

    @Test
    void of_rejectsBlankFeature() {
        assertThatThrownBy(() -> InternalRoutePrefixes.of(" "))
                                                               .isInstanceOf(IllegalArgumentException.class)
                                                               .hasMessageContaining("featureSnakeCase");
    }

    @Test
    void segment_returnsFirstPathToken() {
        assertThat(InternalRoutePrefixes.segment(InternalRoutePrefixes.CUSTOM_PAGE)).isEqualTo("__custom_page__");
        assertThat(InternalRoutePrefixes.segment("/__activity_pub__/user/alice/inbox")).isEqualTo("__activity_pub__");
    }
}
