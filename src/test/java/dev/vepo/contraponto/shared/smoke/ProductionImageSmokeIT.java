package dev.vepo.contraponto.shared.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.vepo.contraponto.shared.App;

@DockerSmokeWebTest
@EnabledIfSystemProperty(named = "contraponto.smoke.enabled", matches = "true")
class ProductionImageSmokeIT {

    @Test
    void authenticated_authorSubdomainWithSharedSession(WebDriver driver, WebDriverWait wait) {
        loginAsAdmin(driver, wait);
        App.at(driver, wait, DockerSmokeUrls.authorOrigin(SmokeCredentials.ADMIN_USERNAME))
           .access()
           .assertAccessible()
           .openUserMenu()
           .assertMenuIsDisplayed()
           .followUserMenuLink("/writing")
           .assertAccessible();
        assertThat(driver.getCurrentUrl()).contains(DockerSmokeUrls.PLATFORM_HOST);
    }

    @Test
    void authenticated_workspaceOnPlatformHost(WebDriver driver, WebDriverWait wait) {
        var app = loginAsAdmin(driver, wait);
        app.visitPath("/writing")
           .assertAccessible();
        app.writePage()
           .assertEditorMounted();
        app.assertAccessible();
        app.manageBlogs();
        app.assertAccessible();
        app.visitPath("/administration/users")
           .assertAccessible();
    }

    @Test
    void authorSubdomain_anonymousBlogHomeIsReachable(WebDriver driver, WebDriverWait wait) {
        App.at(driver, wait, DockerSmokeUrls.authorOrigin(SmokeCredentials.ADMIN_USERNAME))
           .access()
           .assertAccessible();
    }

    @Test
    void authorSubdomain_anonymousWriteRedirectsToPlatformSignIn(WebDriver driver, WebDriverWait wait) {
        App.at(driver, wait, DockerSmokeUrls.authorOrigin(SmokeCredentials.ADMIN_USERNAME))
           .visitPath("/write")
           .assertAccessible();
        assertThat(driver.getCurrentUrl()).contains(DockerSmokeUrls.PLATFORM_HOST);
    }

    @Test
    void authorSubdomain_discoveryRedirectsToPlatform(WebDriver driver, WebDriverWait wait) {
        App.at(driver, wait, DockerSmokeUrls.authorOrigin(SmokeCredentials.ADMIN_USERNAME))
           .visitPath("/authors")
           .assertAccessible();
        assertThat(driver.getCurrentUrl()).contains(DockerSmokeUrls.PLATFORM_HOST);
    }

    private App loginAsAdmin(WebDriver driver, WebDriverWait wait) {
        return App.at(driver, wait, DockerSmokeUrls.platform())
                  .access()
                  .loginModal()
                  .useLogin(SmokeCredentials.ADMIN_USERNAME)
                  .usePassword(SmokeCredentials.ADMIN_PASSWORD)
                  .submit()
                  .assertModalWasClosed()
                  .assertAccessible();
    }

    @Test
    void platformHost_anonymousSurfacesAreReachable(App app) {
        app.access()
           .assertAccessible()
           .visitPath("/authors")
           .assertAccessible()
           .visitPath("/admin")
           .assertAccessible();
    }

    @Test
    void platformHost_signInModalIsReachable(App app) {
        app.access()
           .loginModal()
           .assertSubmitReachable()
           .closeModal();
        app.assertAccessible();
    }
}
