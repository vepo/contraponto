package dev.vepo.contraponto.shared;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.custompage.CustomPageCache;
import dev.vepo.contraponto.custompage.PagePlacement;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;

@WebTest
class MobileLayoutWebTest {

    private User author;
    private Post featuredPost;
    private User notificationUser;

    @Test
    void guestHomeShowsSingleColumnGridAndHeaderControls(App app) {
        app.useMobileViewport()
           .access()
           .assertHeaderIsDisplayed()
           .assertHeaderControlsVisible()
           .assertSingleColumnPostsGrid();
    }

    @Test
    void guestOpensSearchModalOnMobile(App app) {
        app.useMobileViewport()
           .access()
           .searchModal()
           .assertInputVisible();
    }

    @Test
    void guestOpensSidebarDrawerOnMobile(App app) {
        var sidebarPage = Given.customPage()
                               .withSlug("/mobile-sidebar")
                               .withTitle("Mobile Sidebar Page")
                               .withSection("Explore")
                               .withContent("<p>Sidebar navigation test.</p>")
                               .withPlacement(PagePlacement.SIDEBAR)
                               .persist();
        Given.inject(CustomPageCache.class).refresh(sidebarPage.getId());

        app.useMobileViewport()
           .access()
           .clickSidebarMenu()
           .assertSidebarOpen()
           .assertSidebarLinkVisible();
    }

    @Test
    void guestOpensSignInModalOnMobile(App app) {
        app.useMobileViewport()
           .access()
           .loginModal()
           .assertModalIsOpen()
           .assertSubmitReachable();
    }

    @Test
    void loggedInUserReadsPostWithoutHorizontalScroll(App app) {
        app.useMobileViewport()
           .login(author)
           .goTo(featuredPost);
        app.assertNoHorizontalPageScroll()
           .assertDocumentTitleContains("Mobile Featured Post");
    }

    @Test
    void manageHubShowsHorizontalTabsOnMobile(App app) {
        app.useMobileViewport()
           .login(author)
           .dashboard();
        app.assertHubNavTabsVisible();
    }

    @Test
    void notificationOverlayFitsMobileViewport(App app) {
        app.useMobileViewport()
           .login(notificationUser)
           .clickNotificationBell()
           .assertNotificationOverlayWithinViewport();
    }

    @AfterEach
    void restoreDesktopViewport(App app) {
        app.useDesktopViewport();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("mobile-author")
                      .withEmail("mobile-author@test.com")
                      .withName("Mobile Author")
                      .withPassword("password123")
                      .persist();
        featuredPost = Given.post()
                            .withTitle("Mobile Featured Post")
                            .withSlug("mobile-featured-post")
                            .withContent("Mobile post body for reading tests.")
                            .withAuthor(author)
                            .withFeatured(true)
                            .persist();
        Given.post()
             .withTitle("Mobile Grid Post")
             .withSlug("mobile-grid-post")
             .withContent("Second featured grid item.")
             .withAuthor(author)
             .withFeatured(true)
             .persist();
        notificationUser = Given.user()
                                .withUsername("mobile-notify")
                                .withEmail("mobile-notify@test.com")
                                .withName("Mobile Notify")
                                .withPassword("password123")
                                .persist();
    }

    @Test
    void userMenuFitsMobileViewport(App app) {
        app.useMobileViewport()
           .login(author)
           .access();
        app.assertUserMenuTriggerCompactOnMobile()
           .assertUserMenuWithinViewport();
    }

    @Test
    void writeActionsFitMobileViewport(App app) {
        app.useMobileViewport()
           .login(author)
           .writePage();
        app.assertWriteActionsFitMobileViewport();
    }

    @Test
    void writeEditorIsUsableOnMobile(App app) {
        app.useMobileViewport()
           .login(author)
           .writePage();
        app.assertWriteEditorVisible();
    }
}
