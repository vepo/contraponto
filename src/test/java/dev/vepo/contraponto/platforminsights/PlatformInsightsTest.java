package dev.vepo.contraponto.platforminsights;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestTimes;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.persistence.EntityManager;

@WebTest
class PlatformInsightsTest {

    private static final String ADMIN_EMAIL = "insightsadmin@example.com";
    private static final String ADMIN_PASSWORD = "insightsAdminPass123";
    private static final String ADMIN_USERNAME = "insightsadmin";

    @TestHTTPResource("/")
    URL baseUrl;

    private User adminUser;

    @Test
    void adminCanOpenPlatformInsights(App app) {
        var post = publishedPost();
        seedViews(post, TestTimes.REFERENCE, 3);

        app.login(adminUser)
           .platformInsights()
           .assertTitle("Platform insights");

        app.goToPlatformInsights(TestTimes.REFERENCE_MONTH.getYear(), TestTimes.REFERENCE_MONTH.getMonthValue())
           .assertAnalyticsLoaded()
           .assertPostViewsSummary(3)
           .assertUniqueVisitorsLegendVisible();
    }

    @Test
    void analyticsFragmentIncludesFourCharts() {
        var sessionId = Given.inject(LoggedUserProvider.class).login(adminUser).getSessionId();

        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/administration/insights/components/analytics?year=%d&month=%d".formatted(TestTimes.REFERENCE_MONTH.getYear(),
                                                                                               TestTimes.REFERENCE_MONTH.getMonthValue()))
               .then()
               .statusCode(200)
               .body(containsString("Daily post views"))
               .body(containsString("Daily unique visitors"))
               .body(containsString("Daily highlights"))
               .body(containsString("Daily comments"));
    }

    @Test
    void analyticsFragmentRequiresAdminAccess() {
        var regular = Given.user()
                           .withUsername("inspfrag")
                           .withEmail("inspfrag@example.com")
                           .withName("Insights Fragment")
                           .withPassword("regularPass123")
                           .persist();
        var sessionId = Given.inject(LoggedUserProvider.class).login(regular).getSessionId();

        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        given().cookie(LoginEndpoint.SESSION_COOKIE_NAME, sessionId)
               .get("/administration/insights/components/analytics")
               .then()
               .statusCode(403);
    }

    @Test
    void nonAdminCannotAccessPlatformInsights(App app) {
        var regular = Given.user()
                           .withUsername("inspregular")
                           .withEmail("inspregular@example.com")
                           .withName("Insights Regular")
                           .withPassword("regularPass123")
                           .persist();

        app.login(regular)
           .platformInsights()
           .assertManagePageNotLoaded();
    }

    private Post publishedPost() {
        return Given.post()
                    .withAuthor(adminUser)
                    .withTitle("Insights Post")
                    .withContent("Body")
                    .withSlug("insights-post")
                    .withPublished(true)
                    .persist();
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
                  .setParameter("sessionId", "insights-view-" + viewedAt + "-" + i)
                  .setParameter("viewedAt", viewedAt.plusMinutes(i))
                  .executeUpdate();
            }
        });
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        adminUser = Given.user()
                         .withUsername(ADMIN_USERNAME)
                         .withEmail(ADMIN_EMAIL)
                         .withPassword(ADMIN_PASSWORD)
                         .withName("Insights Admin")
                         .withRoles(Role.USER_ADMINISTRATOR, Role.USER)
                         .persist();
    }
}
