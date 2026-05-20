package dev.vepo.contraponto.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class BreadcrumbServiceTest {

    @Inject
    BreadcrumbService breadcrumbService;

    @Test
    void manageBlogEditTrail() {
        var owner = new User();
        owner.setUsername("alice");
        owner.setName("Alice");
        var blog = new Blog(owner, "notes", "Notes", "desc");
        blog.setId(42L);

        var trail = breadcrumbService.manageBlogEdit(blog);

        assertThat(trail.items()).hasSize(3);
        assertThat(trail.items().get(0).label()).isEqualTo("Manage");
        assertThat(trail.items().get(0).href()).isEqualTo("/manage/dashboard");
        assertThat(trail.items().get(1).href()).isEqualTo("/manage/blogs");
        assertThat(trail.items().get(2).label()).isEqualTo("Notes");
        assertThat(trail.items().get(2).isCurrent()).isTrue();
    }

    @Test
    void writingBlogEditTrail() {
        var owner = new User();
        owner.setUsername("alice");
        owner.setName("Alice");
        var blog = new Blog(owner, "notes", "Notes", "desc");
        blog.setId(42L);

        var trail = breadcrumbService.writingBlogEdit(blog);

        assertThat(trail.items()).hasSize(3);
        assertThat(trail.items().get(0).label()).isEqualTo("Writing");
        assertThat(trail.items().get(1).href()).isEqualTo("/writing/blogs");
        assertThat(trail.items().get(2).label()).isEqualTo("Notes");
    }

    @Test
    void writingLibraryTrail() {
        var trail = breadcrumbService.writingLibrary();

        assertThat(trail.items()).hasSize(2);
        assertThat(trail.items().get(1).label()).isEqualTo("Library");
        assertThat(trail.items().get(1).isCurrent()).isTrue();
    }
}
