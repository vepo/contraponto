package dev.vepo.contraponto.dashboard;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

@UnitTest
class DailyMetricTest {

    @Test
    void barHeightPercentReturnsFullHeightWhenMaxIsZero() {
        var metric = new DailyMetric(LocalDate.of(2026, 5, 1), 5);
        assertThat(metric.barHeightPercent(0)).isEqualTo(100);
    }

    @Test
    void barHeightPercentReturnsZeroWhenCountIsZero() {
        var metric = new DailyMetric(LocalDate.of(2026, 5, 1), 0);
        assertThat(metric.barHeightPercent(10)).isZero();
    }

    @Test
    void barHeightPercentScalesCountAgainstMaxWithMinimumBar() {
        var metric = new DailyMetric(LocalDate.of(2026, 5, 1), 1);
        assertThat(metric.barHeightPercent(100)).isEqualTo(4);
        assertThat(metric.barHeightPercent(4)).isEqualTo(25);
    }
}
