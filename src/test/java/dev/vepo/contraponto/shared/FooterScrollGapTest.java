package dev.vepo.contraponto.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

@WebPlatformTest
class FooterScrollGapTest {

    @Test
    void homePageHasNoScrollableSpaceBelowFooter(App app, WebDriver driver) throws Exception {
        app.access();

        Number extra = (Number) ((JavascriptExecutor) driver).executeScript("""
                                                                            const footer = document.querySelector('.site-footer');
                                                                            const r = footer.getBoundingClientRect();
                                                                            return document.documentElement.scrollHeight - (r.bottom + window.scrollY);
                                                                            """);

        assertThat(extra.doubleValue()).isLessThan(1.0);
    }
}
