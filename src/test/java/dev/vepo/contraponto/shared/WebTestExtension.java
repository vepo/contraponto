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

public class WebTestExtension implements BeforeAllCallback, AfterTestExecutionCallback, AfterAllCallback, ParameterResolver {

    private static final Logger logger = LoggerFactory.getLogger(WebTestExtension.class);
    private WebDriver driver;
    private WebDriverWait wait;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        // options.addArguments("--headless");
        options.addArguments("--allow-file-access-from-files");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.setCapability("goog:loggingPrefs", Map.of("browser", "ALL"));
        driver = new ChromeDriver(options);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        logger.info("Closing Chrome driver...");
        driver.close();
        logger.info("Chrome driver closed!");
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        logger.info("Navigate to an empty page...");
        driver.manage().deleteAllCookies();
        driver.manage().logs().get(LogType.BROWSER).getAll()
              .forEach(logEntry -> logger.info("Browser console: {}", logEntry.getMessage()));
        driver.get("about:blank");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(WebDriver.class) ||
                parameterContext.getParameter().getType().isAssignableFrom(WebDriverWait.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().isAssignableFrom(WebDriver.class)) {
            return this.driver;
        } else if (parameterContext.getParameter().getType().isAssignableFrom(WebDriverWait.class)) {
            if (Objects.isNull(this.wait)) {
                this.wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            }
            return this.wait;
        } else {
            throw new ParameterResolutionException("Parameter not implemented!!! class=%s".formatted(parameterContext.getParameter().getType()));
        }
    }
}