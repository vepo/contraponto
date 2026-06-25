package dev.vepo.contraponto.shared;

import java.time.Duration;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChromeTestDriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(ChromeTestDriverFactory.class);

    public static WebDriver createDriver(String... extraArguments) {
        var options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        for (var extraArgument : extraArguments) {
            options.addArguments(extraArgument);
        }
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
        return new ChromeDriver(options);
    }

    public static WebDriverWait createSmokeWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public static WebDriverWait createWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    private ChromeTestDriverFactory() {}
}
