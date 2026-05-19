package dev.vepo.contraponto.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.User;

@WebTest
class I18nWebTest {

    private static final String PASSWORD = "password123";
    private User user;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        user = Given.user()
                    .withUsername("i18nuser")
                    .withEmail("i18nuser@test.com")
                    .withName("I18n User")
                    .withPassword(PASSWORD)
                    .persist();
    }

    @Test
    void shouldShowEnglishMenuLabelsWhenLocaleIsEn(App app) {
        app.login(user)
           .openUserMenu();

        assertThat(app.userMenuLinkText("/writing")).isEqualTo("Writing");
    }

    @Test
    void shouldShowPortugueseMenuLabelsWhenLocaleIsPtBr(App app) {
        app.login(user)
           .setLocaleCookie(LocalePreference.DEFAULT_LOCALE)
           .refresh()
           .openUserMenu();

        assertThat(app.userMenuLinkText("/writing")).isEqualTo("Escrita");
    }
}
