package dev.vepo.contraponto.notification;

import java.util.List;
import java.util.stream.Collectors;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@ApplicationScoped
@Path("/subscriptions")
public class SubscriptionEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance subscriptions(LoggedUser user, Links links, List<SubscriptionRow> rows);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogAudienceRepository audienceRepository;
    private final BlogAudienceComponentEndpoint audienceComponentEndpoint;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public SubscriptionEndpoint(BlogAudienceRepository audienceRepository,
                                BlogAudienceComponentEndpoint audienceComponentEndpoint,
                                CustomPageRepository customPageRepository,
                                LoggedUser loggedUser) {
        this.audienceRepository = audienceRepository;
        this.audienceComponentEndpoint = audienceComponentEndpoint;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        List<SubscriptionRow> rows = audienceRepository.findByUserId(loggedUser.getId())
                                                       .stream()
                                                       .map(a -> new SubscriptionRow(a.getBlog(),
                                                                                     audienceComponentEndpoint.buildView(a.getBlog())))
                                                       .collect(Collectors.toList());
        return Templates.subscriptions(loggedUser, customPageRepository.loadLinks(), rows);
    }
}
