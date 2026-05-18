package dev.vepo.contraponto.dashboard;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record MonthSeries(int year, int month, List<DailyMetric> days, long total) {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public YearMonth yearMonth() {
        return YearMonth.of(year, month);
    }

    public String label() {
        return yearMonth().format(LABEL);
    }

    public long maxDailyCount() {
        return days.stream().mapToLong(DailyMetric::count).max().orElse(0L);
    }

    public long countForDayOfMonth(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > days.size()) {
            return 0L;
        }
        return days.get(dayOfMonth - 1).count();
    }

    public int barHeightForDayOfMonth(int dayOfMonth, long max) {
        if (dayOfMonth < 1 || dayOfMonth > yearMonth().lengthOfMonth()) {
            return 0;
        }
        return new DailyMetric(yearMonth().atDay(dayOfMonth), countForDayOfMonth(dayOfMonth)).barHeightPercent(max);
    }
}
