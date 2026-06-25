package dev.vepo.contraponto.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebPlatformTest;
import dev.vepo.contraponto.user.User;

@WebPlatformTest
class I18nWebTest {

    private static final String PASSWORD = "password123";
    private User user;
    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        user = Given.user()
                    .withUsername("i18nuser")
                    .withEmail("i18nuser@test.com")
                    .withName("I18n User")
                    .withPassword(PASSWORD)
                    .persist();
        author = Given.user()
                      .withUsername("i18nauthor")
                      .withEmail("i18nauthor@test.com")
                      .withName("I18n Author")
                      .withPassword(PASSWORD)
                      .persist();
        post = Given.post()
                    .withAuthor(author)
                    .withTitle("I18n Post")
                    .withSlug("i18n-post")
                    .withContent("Enough words for a published post about internationalization testing.")
                    .persist();
    }

    @Test
    void shouldKeepMenuIconsWhenLocaleIsEn(App app) {
        app.login(user)
           .setLocaleCookie("en")
           .refresh()
           .openUserMenu();

        app.assertUserMenuItemHasIcon("/writing");
        assertThat(app.userMenuLinkText("/writing")).isEqualTo("Writing");
    }

    @Test
    void shouldNotFillCommentTextareaWhenLocaleIsEs(App app) {
        app.login(user)
           .visitPost(author.getUsername(), post.getSlug())
           .setLocaleCookie("es")
           .refresh()
           .assertCommentsFormVisible();

        assertThat(app.commentTextareaValue()).isEmpty();
        assertThat(app.commentTextareaPlaceholder()).isEqualTo("Escribe un comentario…");
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
