package dev.vepo.contraponto.platforminsights;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.comment.PostCommentRepository;
import dev.vepo.contraponto.dashboard.DailyMetric;
import dev.vepo.contraponto.dashboard.MonthSeries;
import dev.vepo.contraponto.highlight.PostTextHighlightRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.user.UserAccess;
import dev.vepo.contraponto.view.DailyUniqueVisitors;
import dev.vepo.contraponto.view.ViewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

@ApplicationScoped
public class PlatformInsightsService {

    private record MonthRange(LocalDateTime start, LocalDateTime end) {}

    private final ViewRepository viewRepository;
    private final PostTextHighlightRepository highlightRepository;
    private final PostCommentRepository commentRepository;
    private final UserAccess userAccess;
    private final LoggedUser loggedUser;

    @Inject
    public PlatformInsightsService(ViewRepository viewRepository,
                                   PostTextHighlightRepository highlightRepository,
                                   PostCommentRepository commentRepository,
                                   UserAccess userAccess,
                                   LoggedUser loggedUser) {
        this.viewRepository = viewRepository;
        this.highlightRepository = highlightRepository;
        this.commentRepository = commentRepository;
        this.userAccess = userAccess;
        this.loggedUser = loggedUser;
    }

    private MonthSeries buildCountSeries(YearMonth yearMonth, Map<LocalDate, Long> counts) {
        List<DailyMetric> days = new ArrayList<>();
        long total = 0L;
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            long count = counts.getOrDefault(date, 0L);
            days.add(new DailyMetric(date, count));
            total += count;
        }
        return new MonthSeries(yearMonth.getYear(), yearMonth.getMonthValue(), List.copyOf(days), total);
    }

    private VisitorSplitMonthSeries buildVisitorSplitSeries(YearMonth yearMonth,
                                                            Map<LocalDate, DailyUniqueVisitors> counts) {
        List<DailyVisitorSplit> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            DailyUniqueVisitors split = counts.getOrDefault(date, new DailyUniqueVisitors(0L, 0L));
            days.add(new DailyVisitorSplit(date, split.registeredVisitors(), split.guestVisitors()));
        }
        return new VisitorSplitMonthSeries(yearMonth.getYear(), yearMonth.getMonthValue(), List.copyOf(days));
    }

    public PlatformInsights load(Integer year, Integer month) {
        requirePlatformInsightsAccess();
        YearMonth selected = resolveYearMonth(year, month);
        YearMonth now = YearMonth.now(ZoneId.systemDefault());
        var range = monthRange(selected);

        MonthSeries postViews = buildCountSeries(selected,
                                                 viewRepository.countDailyPlatform(range.start(), range.end()));
        VisitorSplitMonthSeries uniqueVisitors = buildVisitorSplitSeries(selected,
                                                                         viewRepository.countDailyUniqueVisitorsPlatform(range.start(),
                                                                                                                         range.end()));
        DailyUniqueVisitors monthlyUniqueVisitors = viewRepository.countMonthlyUniqueVisitorsPlatform(range.start(),
                                                                                                      range.end());
        MonthSeries highlightsAdded = buildCountSeries(selected,
                                                       highlightRepository.countDailyPlatform(range.start(), range.end()));
        MonthSeries commentsCreated = buildCountSeries(selected,
                                                       commentRepository.countDailyPlatform(range.start(), range.end()));

        return new PlatformInsights(selected.getYear(),
                                    selected.getMonthValue(),
                                    selected.isBefore(now),
                                    postViews,
                                    uniqueVisitors,
                                    monthlyUniqueVisitors,
                                    highlightsAdded,
                                    commentsCreated);
    }

    private MonthRange monthRange(YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        return new MonthRange(start, end);
    }

    private void requirePlatformInsightsAccess() {
        if (!userAccess.canManageUsers(loggedUser)) {
            throw new ForbiddenException("Platform insights access denied");
        }
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        YearMonth now = YearMonth.now(ZoneId.systemDefault());
        if (year == null || month == null) {
            return now;
        }
        YearMonth selected = YearMonth.of(year, month);
        if (selected.isAfter(now)) {
            return now;
        }
        return selected;
    }
}
