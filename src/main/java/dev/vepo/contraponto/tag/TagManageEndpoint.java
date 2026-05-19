package dev.vepo.contraponto.tag;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class TagManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance panel(Page<TagRow> tags, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final TagRepository tagRepository;

    @Inject
    public TagManageEndpoint(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Page<TagRow> listPage(int page) {
        return tagRepository.findPageForManagement(PageQuery.forGrid(20, page)).map(TagRow::from);
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        return Templates.panel(listPage(page), basePath);
    }

}
