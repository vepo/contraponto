package dev.vepo.contraponto.platforminsights;

import java.time.YearMonth;
import java.util.List;

public record VisitorSplitMonthSeries(int year, int month, List<DailyVisitorSplit> days) {

    public YearMonth yearMonth() {
        return YearMonth.of(year, month);
    }

    public long maxDailyCount() {
        return days.stream()
                   .mapToLong(day -> Math.max(day.registeredVisitors(), day.guestVisitors()))
                   .max()
                   .orElse(0L);
    }
}
