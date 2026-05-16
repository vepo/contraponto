package dev.vepo.contraponto.tag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.App;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.WebTest;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.User;

@WebTest
class TagManageTest {

    private User editor;
    private User reader;

    @Test
    void editorCanBrowseAndEditTags(App app) {
        app.login(editor)
           .tagsManage()
           .assertTitle("Manage Tags")
           .assertTagListed("news")
           .clickEdit("news")
           .fillName("Headlines")
           .submit();

        app.tagsManage()
           .assertTagListed("Headlines");
    }

    @Test
    void plainUserCannotAccessTagManage(App app) {
        app.login(reader)
           .tagsManage()
           .assertManagePageNotLoaded();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        editor = Given.user()
                      .withUsername("tageditor")
                      .withEmail("editor@example.com")
                      .withPassword("editorPw1")
                      .withName("Tag Editor")
                      .withRole(Role.EDITOR)
                      .persist();
        reader = Given.user()
                      .withUsername("tagreader")
                      .withEmail("reader@example.com")
                      .withPassword("readerPw1")
                      .withName("Tag Reader")
                      .persist();

        Given.post()
             .withTitle("Tagged Article")
             .withSlug("tagged-post")
             .withDescription("d")
             .withContent("body")
             .withAuthor(editor)
             .withTags("news", "other")
             .persist();
    }
}
