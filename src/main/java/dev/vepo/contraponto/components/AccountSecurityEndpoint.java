package dev.vepo.contraponto.components;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class AccountSecurityEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(User user,
                                                    boolean emailVerified,
                                                    boolean invalidToken);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;

    @Inject
    public AccountSecurityEndpoint(LoggedUser loggedUser, UserRepository userRepository) {
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
    }

    public TemplateInstance renderHubPanel(boolean emailVerified, String error) {
        var user = userRepository.findById(loggedUser.getId()).orElseThrow();
        boolean invalidToken = "invalid-token".equals(error);
        return Templates.panel(user, emailVerified, invalidToken);
    }
}
