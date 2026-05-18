package dev.vepo.contraponto.dashboard;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.persistence.EntityManager;

@WebTest
class DashboardAnalyticsTest {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    @TestHTTPResource("/")
    URL baseUrl;

    private User testUser;

    @Test
    void compareAnalyticsFragmentIncludesLegend() {
        var blogId = testUser.getDefaultBlog().getId();
        var sessionId = Given.inject(LoggedUserProvider.class).login(testUser).getSessionId();

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/dashboard/components/analytics?blogId=" + blogId + "&compare=true")
               .then()
               .statusCode(200)
               .body(containsString("dashboard-chart__legend"));
    }

    @Test
    void dashboardBlogSelectorChangesMetrics(dev.vepo.contraponto.shared.App app) {
        var secondaryBlog = Given.blog()
                                 .withUser(testUser)
                                 .withSlug("secondary-blog")
                                 .withName("Secondary Blog")
                                 .withDescription("Second blog")
                                 .persist();
        var post = Given.post()
                        .withTitle("Secondary Post")
                        .withContent("Content")
                        .withBlog(secondaryBlog)
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();
        seedViews(post, LocalDateTime.now(), 4);

        app.login(testUser)
           .dashboard()
           .assertAnalyticsLoaded()
           .assertViewsSummary(0)
           .selectBlog("Secondary Blog")
           .assertViewsSummary(4);
    }

    @Test
    void dashboardCompareShowsLegend(dev.vepo.contraponto.shared.App app) {
        var blogId = testUser.getDefaultBlog().getId();
        app.login(testUser)
           .dashboard()
           .assertAnalyticsLoaded()
           .enableCompareViews()
           .assertCompareLegendVisible();

        app.goToAnalyticsFragment(blogId, true)
           .assertCompareLegendVisible();
    }

    @Test
    void dashboardPreviousMonthNavigation(dev.vepo.contraponto.shared.App app) {
        var post = Given.post()
                        .withTitle("Last Month Post")
                        .withContent("Content")
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();
        var lastMonth = YearMonth.now().minusMonths(1);
        seedViews(post, lastMonth.atDay(15).atTime(12, 0), 2);

        app.login(testUser)
           .dashboard()
           .assertAnalyticsLoaded()
           .assertViewsSummary(0)
           .clickPreviousMonth()
           .assertMonthLabel(lastMonth.format(MONTH_LABEL))
           .assertViewsSummary(2);
    }

    @Test
    void dashboardShowsNewFollowersSummary(dev.vepo.contraponto.shared.App app) {
        var blog = testUser.getDefaultBlog();
        seedFollowNotification(blog, LocalDateTime.now());

        app.login(testUser)
           .dashboard()
           .assertAnalyticsLoaded()
           .assertNewFollowersSummary(1, 0);
    }

    @Test
    void dashboardShowsViewsThisMonth(dev.vepo.contraponto.shared.App app) {
        var post = Given.post()
                        .withTitle("Analytics Post")
                        .withContent("Content")
                        .withAuthor(testUser)
                        .withPublished(true)
                        .persist();
        var blog = post.getBlog();
        seedViews(post, LocalDateTime.now(), 3);

        app.login(testUser)
           .dashboard()
           .assertAnalyticsLoaded()
           .assertViewsSummary(3)
           .selectBlog(blog.getName())
           .assertViewsSummary(3);
    }

    private void seedFollowNotification(Blog blog, LocalDateTime createdAt) {
        Given.transaction(() -> {
            var em = Given.inject(EntityManager.class);
            em.createNativeQuery("""
                                 INSERT INTO tb_notifications (recipient_user_id, type, blog_id, read, created_at)
                                 VALUES (:recipientId, 'NEW_FOLLOW', :blogId, false, :createdAt)
                                 """)
              .setParameter("recipientId", blog.getOwner().getId())
              .setParameter("blogId", blog.getId())
              .setParameter("createdAt", createdAt)
              .executeUpdate();
        });
    }

    private void seedViews(Post post, LocalDateTime viewedAt, int count) {
        Given.transaction(() -> {
            var em = Given.inject(EntityManager.class);
            for (int i = 0; i < count; i++) {
                em.createNativeQuery("""
                                     INSERT INTO tb_views (post_id, session_id, viewed_at)
                                     VALUES (:postId, :sessionId, :viewedAt)
                                     """)
                  .setParameter("postId", post.getId())
                  .setParameter("sessionId", "test-view-" + viewedAt + "-" + i)
                  .setParameter("viewedAt", viewedAt.plusMinutes(i))
                  .executeUpdate();
            }
        });
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        testUser = Given.user()
                        .withUsername("analyticsuser")
                        .withEmail("analytics@test.com")
                        .withName("Analytics User")
                        .withPassword("Password123!")
                        .persist();
        follower = Given.user()
                        .withUsername("followeruser")
                        .withEmail("follower@test.com")
                        .withName("Follower User")
                        .withPassword("Password123!")
                        .persist();
    }

    @Test
    void unauthorizedBlogAnalyticsReturns404() {
        var otherUser = Given.user()
                             .withUsername("otheranalytics")
                             .withEmail("otheranalytics@test.com")
                             .withName("Other")
                             .withPassword("Password123!")
                             .persist();
        var otherBlogId = otherUser.getDefaultBlog().getId();
        var sessionId = Given.inject(LoggedUserProvider.class).login(testUser).getSessionId();

        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/dashboard/components/analytics?blogId=" + otherBlogId)
               .then()
               .statusCode(404);
    }
}
