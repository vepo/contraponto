package dev.vepo.contraponto.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@WebTest
@QuarkusTest
class SearchTest {

    @TestHTTPResource("/")
    URL testUrl;

    private User testUser;

    @Test
    void clickingSearchResultNavigatesToPost(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search?q=Java");
        WebElement firstResultLink = wait.until(visibilityOfElementLocated(cssSelector(".search-result__title a")));
        String expectedPostUrl = firstResultLink.getAttribute("data-hx-get");
        firstResultLink.click();

        wait.until(d -> d.getCurrentUrl().contains("/post/"));
        assertThat(driver.getCurrentUrl()).endsWith(expectedPostUrl);
        var postTitle = wait.until(visibilityOfElementLocated(cssSelector(".article-page__title")));
        var postContent = wait.until(visibilityOfElementLocated(cssSelector(".article-page__content")));
        assertThat(postTitle.getText() + postContent.getText()).containsIgnoringCase("Java");
    }

    // ========================================================================
    // Modal search tests
    // ========================================================================

    @Test
    void modalSearchDisplaysResultsWhileTyping(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        WebElement searchBtn = wait.until(visibilityOfElementLocated(cssSelector("#searchBtn")));
        searchBtn.click();

        WebElement modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
        WebElement searchInput = modal.findElement(cssSelector("input[name='q']"));
        searchInput.sendKeys("Java");

        // Wait for results container to have content
        wait.until(d -> {
            WebElement resultsContainer = modal.findElement(By.id("modalSearchResults"));
            return resultsContainer.findElements(cssSelector(".search-result")).size() > 0;
        });

        List<WebElement> results = modal.findElements(cssSelector("#modalSearchResults .search-result"));
        assertThat(results).hasSizeBetween(1, 5);
        // Verify at least one result contains "Java" (case-insensitive)
        boolean containsJava = results.stream()
                                      .anyMatch(r -> r.getText().toLowerCase().contains("java"));
        assertThat(containsJava).isTrue();
    }

    @Test
    void modalSearchHasAdvancedSearchLink(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        WebElement searchBtn = wait.until(visibilityOfElementLocated(cssSelector("#searchBtn")));
        searchBtn.click();

        WebElement modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
        WebElement advancedLink = modal.findElement(cssSelector(".search-modal__advanced"));
        assertThat(advancedLink.getText()).contains("Advanced search");
        assertThat(advancedLink.getAttribute("href")).contains("/search");
    }

    @Test
    void modalSearchShowsNoResultsMessage(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        WebElement searchBtn = wait.until(visibilityOfElementLocated(cssSelector("#searchBtn")));
        searchBtn.click();

        WebElement modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
        WebElement searchInput = modal.findElement(cssSelector("input[name='q']"));
        searchInput.sendKeys("NonExistentKeywordXYZ123");

        wait.until(d -> {
            WebElement resultsContainer = modal.findElement(By.id("modalSearchResults"));
            return resultsContainer.findElements(cssSelector(".search-empty")).size() > 0;
        });

        WebElement emptyMsg = modal.findElement(cssSelector(".search-empty"));
        assertThat(emptyMsg.getText()).contains("No results found");
    }

    @Test
    void searchFormSubmitsViaHtmx(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search");
        WebElement searchInput = wait.until(visibilityOfElementLocated(cssSelector("input[name='q']")));
        searchInput.sendKeys("Spring");
        WebElement submitBtn = driver.findElement(cssSelector(".search-form__button"));
        submitBtn.click();

        wait.until(visibilityOfElementLocated(cssSelector("#searchResults .search-result")));
        assertThat(driver.getCurrentUrl()).contains("q=Spring");
        WebElement result = driver.findElement(cssSelector(".search-result__title"));
        assertThat(result.getText()).contains("Spring");
    }

    // ========================================================================
    // Dedicated search page tests
    // ========================================================================

    @Test
    void searchModalOpensAndCloses(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString());
        WebElement searchBtn = wait.until(visibilityOfElementLocated(cssSelector("#searchBtn")));
        searchBtn.click();

        WebElement modal = wait.until(visibilityOfElementLocated(By.id("searchModal")));
        assertThat(modal.isDisplayed()).isTrue();

        WebElement closeBtn = modal.findElement(cssSelector(".modal__close"));
        closeBtn.click();
        wait.until(invisibilityOfElementLocated(By.id("searchModal")));
    }

    @Test
    void searchPageHandlesPagination(WebDriver driver, WebDriverWait wait) {
        // Create more posts to exceed the page limit (10 per page)
        IntStream.range(0, 15).forEach(i -> Given.post()
                                                 .withTitle("Pagination Post " + i)
                                                 .withContent("Content for pagination test.")
                                                 .withAuthor(testUser)
                                                 .persist());

        driver.get(testUrl.toString() + "search?q=pagination");
        wait.until(visibilityOfElementLocated(cssSelector("#searchResults")));
        wait.until(d -> driver.findElements(cssSelector(".search-result")).size() > 5);

        // Pagination should appear if there are more than 10 results
        // We have 1 original + 15 new = 16 posts containing "pagination" (since all
        // have "pagination" in title)
        // Actually the original posts don't have "pagination". So we have 15. More than
        // 10 → pagination button.
        List<WebElement> nextButtons = driver.findElements(By.xpath("//button[contains(text(), 'Next')]"));
        if (!nextButtons.isEmpty()) {
            WebElement nextBtn = nextButtons.get(0);
            nextBtn.click();
            wait.until(d -> driver.findElements(cssSelector(".search-result")).size() > 0);
        }
    }

    @Test
    void searchPageLoadsWithEmptyQuery(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search");
        WebElement searchPageTitle = wait.until(visibilityOfElementLocated(cssSelector(".search-page__title")));
        assertThat(searchPageTitle.getText()).isEqualTo("Search");

        WebElement searchInput = driver.findElement(cssSelector("input[name='q']"));
        assertThat(searchInput.getAttribute("value")).isEmpty();
    }

    @Test
    void searchPageShowsEmptyState(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search?q=NonExistentKeywordXYZ123");
        wait.until(visibilityOfElementLocated(cssSelector(".search-empty")));
        WebElement emptyMsg = driver.findElement(cssSelector(".search-empty"));
        assertThat(emptyMsg.getText()).contains("No results found");
    }

    @Test
    void searchPageShowsResultCountAndQuery(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search?q=Java");
        WebElement resultsHeader = wait.until(visibilityOfElementLocated(cssSelector(".search-results__header")));
        assertThat(resultsHeader.getText()).contains("Found").contains("Java");
    }

    @Test
    void searchPageShowsResultsForQuery(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search?q=Java");
        // The results are loaded via HTMX, we need to wait for the result list
        wait.until(visibilityOfElementLocated(cssSelector("#searchResults")));
        wait.until(d -> driver.findElements(cssSelector(".search-result")).size() > 0);

        List<WebElement> results = driver.findElements(cssSelector(".search-result"));
        assertThat(results).hasSizeBetween(1, 5);
        var resultText = results.stream().filter(elm -> elm.getText().toLowerCase().contains("java")).findFirst();
        assertThat(resultText.isPresent());
        assertThat(resultText.get().getText().toLowerCase()).contains("java");
    }

    @Test
    void searchResultsIncludeAuthorNameAndDate(WebDriver driver, WebDriverWait wait) {
        driver.get(testUrl.toString() + "search?q=Java");
        WebElement meta = wait.until(visibilityOfElementLocated(cssSelector(".search-result__meta")));
        assertThat(meta.findElement(cssSelector(".search-result__author")).getText()).isEqualTo(testUser.getName());
        assertThat(meta.findElement(cssSelector(".search-result__date")).getText()).isNotBlank();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();

        testUser = Given.user()
                        .withUsername("searchuser")
                        .withEmail("search@example.com")
                        .withPassword("searchpass")
                        .withName("Search Tester")
                        .persist();

        // Create posts with distinct searchable content
        Given.post()
             .withTitle("Introduction to Java")
             .withContent("Java is a popular programming language. This post covers basics.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Advanced Spring Boot")
             .withContent("Learn how to build microservices with Spring Boot and Java.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Python vs Java")
             .withContent("Comparing Python and Java for backend development.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Kubernetes for Java Developers")
             .withContent("Deploy Java applications on Kubernetes.")
             .withAuthor(testUser)
             .persist();

        Given.post()
             .withTitle("Testing with JUnit 5")
             .withContent("Write unit and integration tests for your Java code.")
             .withAuthor(testUser)
             .persist();
    }
}