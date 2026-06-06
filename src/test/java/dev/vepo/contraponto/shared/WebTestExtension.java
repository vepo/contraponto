package dev.vepo.contraponto.shared;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class WebTestExtension implements BeforeAllCallback, AfterTestExecutionCallback, AfterAllCallback, ParameterResolver {

    private static final Logger logger = LoggerFactory.getLogger(WebTestExtension.class);
    private WebDriver driver;
    private WebDriverWait wait;
    private App app;

    @Override
    public void afterAll(ExtensionContext context) {
        if (driver != null) {
            logger.info("Closing Chrome driver...");
            try {
                driver.quit();
            } catch (Exception e) {
                logger.warn("Chrome driver shutdown failed", e);
            }
            driver = null;
            logger.info("Chrome driver closed!");
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        logger.info("Navigate to an empty page...");
        try {
            driver.get(TestHTTPResourceManager.getUri());
            driver.manage().deleteAllCookies();
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                                                            "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch (e) {}");
        } catch (Exception e) {
            logger.warn("Test cleanup failed", e);
        }
        driver.manage().logs().get(LogType.BROWSER).getAll()
              // only log the message, not the entire log entry
              .forEach(logEntry -> logger.info("Browser console: {}", logEntry.getMessage()));
        driver.get("about:blank");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        // Headless Chrome defaults to a small viewport; fixed header/footer then
        // intercept clicks (CI flakiness).
        options.addArguments("--window-size=1920,1080");
        // Enable headless only when running in GitHub Actions
        var githubActions = System.getenv("GITHUB_ACTIONS");
        if ("true".equalsIgnoreCase(githubActions)) {
            options.addArguments("--headless");
            logger.info("Running in headless mode (GitHub Actions detected)");
        } else {
            logger.info("Running with UI (not in GitHub Actions)");
        }
        options.addArguments("--allow-file-access-from-files");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.setCapability("goog:loggingPrefs", Map.of("browser", "ALL"));
        driver = new ChromeDriver(options);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().isAssignableFrom(WebDriver.class)) {
            return this.driver;
        } else if (parameterContext.getParameter().getType().isAssignableFrom(WebDriverWait.class)) {
            if (Objects.isNull(this.wait)) {
                this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            }
            return this.wait;
        } else if (parameterContext.getParameter().getType().isAssignableFrom(App.class)) {
            if (Objects.isNull(app)) {
                if (Objects.isNull(this.wait)) {
                    this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
                }
                this.app = new App(driver, wait);
            }
            return this.app;
        } else {
            throw new ParameterResolutionException("Parameter not implemented!!! class=%s".formatted(parameterContext.getParameter().getType()));
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(WebDriver.class) ||
                parameterContext.getParameter().getType().isAssignableFrom(WebDriverWait.class) ||
                parameterContext.getParameter().getType().isAssignableFrom(App.class);
    }
}
