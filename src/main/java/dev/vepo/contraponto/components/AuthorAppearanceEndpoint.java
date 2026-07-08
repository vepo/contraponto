package dev.vepo.contraponto.components;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import dev.vepo.contraponto.activitypub.actor.ActivityPubAppearanceService;
import dev.vepo.contraponto.activitypub.actor.ActivityPubFederationView;
import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;

@Logged
@ApplicationScoped
public class AuthorAppearanceEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(User user, long mainBlogId, ActivityPubFederationView federationView);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final ActivityPubAppearanceService activityPubAppearanceService;

    @Inject
    public AuthorAppearanceEndpoint(LoggedUser loggedUser,
                                    UserRepository userRepository,
                                    BlogRepository blogRepository,
                                    ActivityPubAppearanceService activityPubAppearanceService) {
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.activityPubAppearanceService = activityPubAppearanceService;
    }

    public TemplateInstance renderHubPanel() {
        var user = userRepository.findById(loggedUser.getId()).orElseThrow();
        var mainBlogId = blogRepository.findMainByOwnerId(user.getId()).map(Blog::getId).orElse(0L);
        var federationView = activityPubAppearanceService.buildView(user);
        return Templates.panel(user, mainBlogId, federationView);
    }
}
