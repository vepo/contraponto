package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class SubscriptionEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(Page<SubscriptionRow> rows, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogAudienceRepository audienceRepository;
    private final BlogAudienceComponentEndpoint audienceComponentEndpoint;
    private final LoggedUser loggedUser;

    @Inject
    public SubscriptionEndpoint(BlogAudienceRepository audienceRepository,
                                BlogAudienceComponentEndpoint audienceComponentEndpoint,
                                LoggedUser loggedUser) {
        this.audienceRepository = audienceRepository;
        this.audienceComponentEndpoint = audienceComponentEndpoint;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        var rows = audienceRepository.findPageByUserId(loggedUser.getId(), PageQuery.forGrid(20, page))
                                     .map(a -> new SubscriptionRow(a.getBlog(),
                                                                   audienceComponentEndpoint.buildView(a.getBlog())));
        return Templates.panel(rows, basePath);
    }
}
