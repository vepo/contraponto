package dev.vepo.contraponto.components;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class AuthorAppearanceEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(User user, long mainBlogId);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;

    @Inject
    public AuthorAppearanceEndpoint(LoggedUser loggedUser,
                                    UserRepository userRepository,
                                    BlogRepository blogRepository) {
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
    }

    public TemplateInstance renderHubPanel() {
        var user = userRepository.findById(loggedUser.getId()).orElseThrow();
        var mainBlogId = blogRepository.findMainByOwnerId(user.getId()).map(b -> b.getId()).orElse(0L);
        return Templates.panel(user, mainBlogId);
    }
}
