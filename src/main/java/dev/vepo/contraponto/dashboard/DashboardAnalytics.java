package dev.vepo.contraponto.dashboard;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import dev.vepo.contraponto.blog.Blog;

public record DashboardAnalytics(Blog blog,
                                 List<Blog> ownedBlogs,
                                 int year,
                                 int month,
                                 boolean compareViews,
                                 boolean canGoNextMonth,
                                 MonthSeries views,
                                 MonthSeries viewsComparison,
                                 MonthSeries readingTime,
                                 MonthSeries newFollowers,
                                 MonthSeries newSubscribers,
                                 long totalFollowers,
                                 long totalSubscribers) {

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

    public long viewsChartMax() {
        if (viewsComparison != null) {
            return Math.max(views.maxDailyCount(), viewsComparison.maxDailyCount());
        }
        return views.maxDailyCount();
    }

    public long readingTimeChartMax() {
        return readingTime.maxDailyCount();
    }

    public long viewsPercentChange() {
        if (viewsComparison == null) {
            return 0L;
        }
        long previous = viewsComparison.total();
        if (previous <= 0) {
            return views.total() > 0 ? 100 : 0;
        }
        return Math.round((views.total() - previous) * 100.0 / previous);
    }
}
