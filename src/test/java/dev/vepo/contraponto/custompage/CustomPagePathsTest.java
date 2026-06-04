package dev.vepo.contraponto.custompage;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.user.User;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;

@UnitTest
class CustomPagePathsTest {

    private static User alice() {
        User u = new User();
        u.setUsername("alice");
        return u;
    }

    private static CustomPage orphanPage(String slug) {
        CustomPage page = new CustomPage();
        page.setSlug(slug);
        page.setBlog(null);
        return page;
    }

    private static CustomPage page(String slug, Blog blog) {
        CustomPage page = new CustomPage();
        page.setSlug(slug);
        page.setBlog(blog);
        return page;
    }

    private static PathSegment seg(String path) {
        return new PathSegment() {
            @Override
            public MultivaluedMap<String, String> getMatrixParameters() {
                return new MultivaluedHashMap<>();
            }

            @Override
            public String getPath() {
                return path;
            }
        };
    }

    private static Blog typedBlog(User owner, String slug, boolean mainBlog) {
        Blog blog = new Blog(owner, slug, slug, slug);
        blog.setMain(mainBlog);
        return blog;
    }

    @Test
    void internalUrlCoversPlacementSwitchEntries() {
        assertThat(CustomPagePaths.internalUrl(PageType.GLOBAL, "glob")).isEqualTo("/_custom_page/global/glob");
        assertThat(CustomPagePaths.internalUrl(PageType.USER, "alice", "p1")).isEqualTo("/_custom_page/user/alice/p1");
        assertThat(CustomPagePaths.internalUrl(PageType.BLOG, "3", "b", "slug")).isEqualTo("/_custom_page/blog/3/b/slug");
        assertThatThrownBy(() -> CustomPagePaths.slug(List.of(seg("page")), PageType.NONE))
                                                                                           .isInstanceOf(IllegalArgumentException.class)
                                                                                           .hasMessageContaining("NONE");
    }

    @Test
    void linksBlogIdRequiresOwningBlogAssociation() {
        CustomPage orphan = orphanPage("/x/");
        assertThatThrownBy(() -> CustomPagePaths.linksBlogId(orphan)).isInstanceOf(IllegalStateException.class);

        Blog b = typedBlog(alice(), "b", false);
        b.setId(77L);
        CustomPage bound = page("z", b);
        assertThat(CustomPagePaths.linksBlogId(bound)).isEqualTo(77L);
    }

    @Test
    void matchPageTypeFiltersReservedSegmentsAndNonePaths() {
        assertThat(CustomPagePaths.matchPageType(null)).isEqualTo(PageType.NONE);
        assertThat(CustomPagePaths.matchPageType(List.of())).isEqualTo(PageType.NONE);
        List<PathSegment> global = List.of(seg("page"), seg("meta"));
        assertThat(CustomPagePaths.matchPageType(global)).isEqualTo(PageType.GLOBAL);

        List<PathSegment> user = List.of(seg("eve"), seg("page"), seg("faq"));
        assertThat(CustomPagePaths.matchPageType(user)).isEqualTo(PageType.USER);

        List<PathSegment> blog = List.of(seg("eve"), seg("travel"), seg("page"), seg("visa"));
        assertThat(CustomPagePaths.matchPageType(blog)).isEqualTo(PageType.BLOG);

        assertThat(CustomPagePaths.matchPageType(List.of(seg("search"), seg("page"), seg("faq")))).isEqualTo(PageType.NONE);
        assertThat(CustomPagePaths.matchPageType(List.of(seg("eve"), seg("search"), seg("page"), seg("visa")))).isEqualTo(PageType.NONE);
        assertThat(CustomPagePaths.matchPageType(List.of(seg("solo")))).isEqualTo(PageType.NONE);
        assertThat(CustomPagePaths.isReservedSegment("api")).isTrue();
        assertThat(CustomPagePaths.isReservedSegment("feed")).isTrue();
        assertThat(CustomPagePaths.isReservedSegment("main-blog")).isTrue();
        assertThat(CustomPagePaths.matchPageType(List.of(seg("eve"), seg("page"), seg("js")))).isEqualTo(PageType.USER);
        assertThat(CustomPagePaths.matchPageType(List.of(seg("eve"), seg("travel"), seg("page"), seg("style")))).isEqualTo(PageType.BLOG);
    }

    @Test
    void pathSlugNormalizesStoredSlugsSafelyForNullBlankOrPrefixes() {
        assertThat(CustomPagePaths.pathSlug(null)).isNull();
        assertThat(CustomPagePaths.pathSlug("")).isEmpty();
        assertThat(CustomPagePaths.pathSlug(" \t")).isEqualTo(" \t"); // isBlank ⇒ returned verbatim
        assertThat(CustomPagePaths.pathSlug("/docs")).isEqualTo("docs");
        assertThat(CustomPagePaths.pathSlug("docs")).isEqualTo("docs");

        assertThat(CustomPagePaths.storedSlug(null)).isNull();
        assertThat(CustomPagePaths.storedSlug("")).isEmpty();
        assertThat(CustomPagePaths.storedSlug("docs")).isEqualTo("/docs");
        assertThat(CustomPagePaths.storedSlug("/docs")).isEqualTo("/docs");
    }

    @Test
    void privateCtorGuardsAgainstInstantiation() throws Exception {
        var ctor = CustomPagePaths.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThatThrownBy(ctor::newInstance).isInstanceOf(InvocationTargetException.class)
                                             .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void publicUrlPrefersOwningBlogLayouts() {
        CustomPage orphaned = orphanPage("/faq/");
        assertThat(CustomPagePaths.publicUrl(orphaned)).isEqualTo("/page/faq/");

        User owner = alice();
        Blog vacation = typedBlog(owner, "vacation", false);
        CustomPage scoped = page("tips", vacation);
        assertThat(CustomPagePaths.publicUrl(scoped)).isEqualTo("/alice/vacation/page/tips");

        Blog primaryLike = typedBlog(owner, "alice", true);
        CustomPage inbox = page("inbox", primaryLike);
        assertThat(CustomPagePaths.publicUrl(inbox)).isEqualTo("/alice/page/inbox");

        assertThat(CustomPagePaths.isMainBlogPage(page("x", vacation))).isFalse();
        assertThat(CustomPagePaths.isMainBlogPage(inbox)).isTrue();
    }

    @Test
    void slugAndUsernameSelectorsUsePathSegments() {
        List<PathSegment> global = List.of(seg("page"), seg("privacy"));
        assertThat(CustomPagePaths.slug(global, PageType.GLOBAL)).isEqualTo("privacy");

        List<PathSegment> user = List.of(seg("alice"), seg("page"), seg("faq"));
        assertThat(CustomPagePaths.username(user)).isEqualTo("alice");
        assertThat(CustomPagePaths.slug(user, PageType.USER)).isEqualTo("faq");

        List<PathSegment> blog = List.of(seg("alice"), seg("travel"), seg("page"), seg("visa"));
        assertThat(CustomPagePaths.blogSlug(blog)).isEqualTo("travel");
        assertThat(CustomPagePaths.slug(blog, PageType.BLOG)).isEqualTo("visa");
    }
}
