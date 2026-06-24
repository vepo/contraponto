package dev.vepo.contraponto.platforminsights;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import dev.vepo.contraponto.dashboard.MonthSeries;
import dev.vepo.contraponto.view.DailyUniqueVisitors;

public record PlatformInsights(int year,
                               int month,
                               boolean canGoNextMonth,
                               MonthSeries postViews,
                               VisitorSplitMonthSeries uniqueVisitors,
                               DailyUniqueVisitors monthlyUniqueVisitors,
                               MonthSeries highlightsAdded,
                               MonthSeries commentsCreated) {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public String monthLabel() {
        return selectedMonth().format(MONTH_LABEL);
    }

    public int previousYear() {
        return selectedMonth().minusMonths(1).getYear();
    }

    public int previousMonth() {
        return selectedMonth().minusMonths(1).getMonthValue();
    }

    public int nextYear() {
        return selectedMonth().plusMonths(1).getYear();
    }

    public int nextMonth() {
        return selectedMonth().plusMonths(1).getMonthValue();
    }

    public YearMonth selectedMonth() {
        return YearMonth.of(year, month);
    }

    public long postViewsChartMax() {
        return postViews.maxDailyCount();
    }

    public long uniqueVisitorsChartMax() {
        return uniqueVisitors.maxDailyCount();
    }

    public long highlightsChartMax() {
        return highlightsAdded.maxDailyCount();
    }

    public long commentsChartMax() {
        return commentsCreated.maxDailyCount();
    }
}
