package dev.vepo.contraponto.dashboard;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

@UnitTest
class MonthSeriesTest {

    @Test
    void countForDayOfMonthReturnsZeroForOutOfRangeDays() {
        var days = List.of(new DailyMetric(LocalDate.of(2026, 5, 1), 3));
        var series = new MonthSeries(2026, 5, days, 3);
        assertThat(series.countForDayOfMonth(0)).isZero();
        assertThat(series.countForDayOfMonth(2)).isZero();
    }

    @Test
    void labelFormatsYearMonthInEnglish() {
        var series = new MonthSeries(2026, 5, List.of(), 0);
        assertThat(series.label()).isEqualTo("May 2026");
        assertThat(series.yearMonth()).isEqualTo(YearMonth.of(2026, 5));
    }

    @Test
    void maxDailyCountAndBarHeightUseSeriesDays() {
        var days = List.of(new DailyMetric(LocalDate.of(2026, 5, 1), 2),
                           new DailyMetric(LocalDate.of(2026, 5, 2), 8));
        var series = new MonthSeries(2026, 5, days, 10);
        assertThat(series.maxDailyCount()).isEqualTo(8);
        assertThat(series.countForDayOfMonth(2)).isEqualTo(8);
        assertThat(series.barHeightForDayOfMonth(2, 8)).isEqualTo(100);
        assertThat(series.barHeightForDayOfMonth(32, 8)).isZero();
    }
}
