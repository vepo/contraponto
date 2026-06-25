package dev.vepo.contraponto.shared.smoke;

import java.util.Objects;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.ChromeTestDriverFactory;
import dev.vepo.contraponto.shared.TestTags;

public final class DockerSmokeWebExtension implements BeforeAllCallback, AfterTestExecutionCallback, AfterAllCallback, ParameterResolver {

    private static final Logger logger = LoggerFactory.getLogger(DockerSmokeWebExtension.class);

    private WebDriver driver;

    private WebDriverWait wait;

    private App platformApp;

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
    public void afterTestExecution(ExtensionContext context) {
        if (driver == null) {
            return;
        }
        logger.info("Navigate to platform home for cookie cleanup...");
        try {
            driver.get(DockerSmokeUrls.platform());
            driver.manage().deleteAllCookies();
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                                                            "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch (e) {}");
        } catch (Exception e) {
            logger.warn("Test cleanup failed", e);
        }
        driver.manage().logs().get(LogType.BROWSER).getAll()
              .forEach(logEntry -> logger.info("Browser console: {}", logEntry.getMessage()));
        driver.get("about:blank");
        platformApp = null;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!context.getTags().contains(TestTags.DOCKER_SMOKE)) {
            return;
        }
        if (driver != null) {
            return;
        }
        driver = ChromeTestDriverFactory.createDriver("--host-resolver-rules=MAP blogs.commit-mestre.test 127.0.0.1, MAP admin.commit-mestre.test 127.0.0.1");
        wait = ChromeTestDriverFactory.createSmokeWait(driver);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().isAssignableFrom(WebDriver.class)) {
            return driver;
        }
        if (parameterContext.getParameter().getType().isAssignableFrom(WebDriverWait.class)) {
            return wait;
        }
        if (parameterContext.getParameter().getType().isAssignableFrom(App.class)) {
            if (Objects.isNull(platformApp)) {
                platformApp = App.at(driver, wait, DockerSmokeUrls.platform());
            }
            return platformApp;
        }
        throw new ParameterResolutionException("Parameter not implemented!!! class=%s".formatted(parameterContext.getParameter().getType()));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(WebDriver.class) ||
                parameterContext.getParameter().getType().isAssignableFrom(WebDriverWait.class) ||
                parameterContext.getParameter().getType().isAssignableFrom(App.class);
    }
}
