package dev.vepo.contraponto.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.BlogAudienceRepository;
import dev.vepo.contraponto.notification.NotificationRepository;
import dev.vepo.contraponto.notification.NotificationType;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.readingtime.ReadingTimeRepository;
import dev.vepo.contraponto.view.ViewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DashboardAnalyticsService {

    private record MonthRange(LocalDateTime start, LocalDateTime end) {}

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final ViewRepository viewRepository;
    private final ReadingTimeRepository readingTimeRepository;
    private final NotificationRepository notificationRepository;
    private final BlogAudienceRepository audienceRepository;

    private final LoggedUser loggedUser;

    @Inject
    public DashboardAnalyticsService(BlogRepository blogRepository,
                                     BlogAccess blogAccess,
                                     ViewRepository viewRepository,
                                     ReadingTimeRepository readingTimeRepository,
                                     NotificationRepository notificationRepository,
                                     BlogAudienceRepository audienceRepository,
                                     LoggedUser loggedUser) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.viewRepository = viewRepository;
        this.readingTimeRepository = readingTimeRepository;
        this.notificationRepository = notificationRepository;
        this.audienceRepository = audienceRepository;
        this.loggedUser = loggedUser;
    }

    private MonthSeries buildNotificationSeries(Blog blog, NotificationType type, YearMonth yearMonth) {
        var range = monthRange(yearMonth);
        Map<LocalDate, Long> counts = notificationRepository.countDailyByBlogAndType(blog.getId(),
                                                                                     blog.getOwner().getId(),
                                                                                     type,
                                                                                     range.start(),
                                                                                     range.end());
        return toMonthSeries(yearMonth, counts);
    }

    private MonthSeries buildReadingTimeSeries(long blogId, YearMonth yearMonth) {
        var range = monthRange(yearMonth);
        Map<LocalDate, Long> counts = readingTimeRepository.countDailySecondsByBlogId(blogId,
                                                                                      range.start(),
                                                                                      range.end());
        return toMonthSeries(yearMonth, counts);
    }

    private MonthSeries buildViewsSeries(long blogId, YearMonth yearMonth) {
        var range = monthRange(yearMonth);
        Map<LocalDate, Long> counts = viewRepository.countDailyByBlogId(blogId, range.start(), range.end());
        return toMonthSeries(yearMonth, counts);
    }

    public DashboardAnalytics load(Long blogId, Integer year, Integer month, boolean compareViews) {
        List<Blog> ownedBlogs = blogRepository.findByOwnerIdForManagement(loggedUser.getId());
        if (ownedBlogs.isEmpty()) {
            throw new NotFoundException("No blogs found");
        }

        Blog blog = resolveBlog(blogId, ownedBlogs);
        YearMonth selected = resolveYearMonth(year, month);
        YearMonth now = YearMonth.now(ZoneId.systemDefault());

        MonthSeries views = buildViewsSeries(blog.getId(), selected);
        MonthSeries viewsComparison = compareViews
                                                   ? buildViewsSeries(blog.getId(), selected.minusMonths(1))
                                                   : null;
        MonthSeries readingTime = buildReadingTimeSeries(blog.getId(), selected);

        MonthSeries newFollowers = buildNotificationSeries(blog,
                                                           NotificationType.NEW_FOLLOW,
                                                           selected);
        MonthSeries newSubscribers = buildNotificationSeries(blog,
                                                             NotificationType.NEW_SUBSCRIBE,
                                                             selected);

        return new DashboardAnalytics(blog,
                                      ownedBlogs,
                                      selected.getYear(),
                                      selected.getMonthValue(),
                                      compareViews,
                                      selected.isBefore(now),
                                      views,
                                      viewsComparison,
                                      readingTime,
                                      newFollowers,
                                      newSubscribers,
                                      audienceRepository.countFollowersByBlogId(blog.getId()),
                                      audienceRepository.countEmailSubscribersByBlogId(blog.getId()));
    }

    private MonthRange monthRange(YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        return new MonthRange(start, end);
    }

    private Blog resolveBlog(Long blogId, List<Blog> ownedBlogs) {
        if (blogId != null) {
            Blog blog = blogRepository.findById(blogId).orElseThrow(NotFoundException::new);
            if (!blogAccess.canEdit(blog, loggedUser)) {
                throw new NotFoundException("Blog not found");
            }
            return blog;
        }
        return blogRepository.findMainByOwnerId(loggedUser.getId())
                             .orElse(ownedBlogs.getFirst());
    }

    public Long resolveDefaultBlogId() {
        List<Blog> ownedBlogs = blogRepository.findByOwnerIdForManagement(loggedUser.getId());
        if (ownedBlogs.isEmpty()) {
            return null;
        }
        return resolveBlog(null, ownedBlogs).getId();
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

    private MonthSeries toMonthSeries(YearMonth yearMonth, Map<LocalDate, Long> counts) {
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
}
