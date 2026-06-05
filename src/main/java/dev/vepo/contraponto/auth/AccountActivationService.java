package dev.vepo.contraponto.auth;

import java.util.Optional;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AccountActivationService {

    private final UserAccountTokenService tokenService;
    private final UserRepository userRepository;
    private final AccountEmailService accountEmailService;
    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public AccountActivationService(UserAccountTokenService tokenService,
                                    UserRepository userRepository,
                                    AccountEmailService accountEmailService,
                                    LoggedUserProvider loggedUserProvider) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.accountEmailService = accountEmailService;
        this.loggedUserProvider = loggedUserProvider;
    }

    @Transactional
    public Optional<LoggedUser> activate(String rawToken) {
        var maybeToken = tokenService.consume(rawToken);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        var token = maybeToken.get();
        if (token.getType() != UserAccountTokenType.ACCOUNT_ACTIVATION) {
            return Optional.empty();
        }

        var user = token.getUser();
        if (user.isActive()) {
            return Optional.empty();
        }

        user.setActive(true);
        userRepository.update(user);
        return Optional.of(loggedUserProvider.login(user));
    }

    @Transactional
    public boolean reportUnauthorizedSignup(String rawToken) {
        var maybeToken = tokenService.consume(rawToken);
        if (maybeToken.isEmpty()) {
            return false;
        }

        var token = maybeToken.get();
        if (token.getType() != UserAccountTokenType.ACCOUNT_ACTIVATION) {
            return false;
        }

        var user = token.getUser();
        if (user.isActive()) {
            return false;
        }

        accountEmailService.sendUnauthorizedSignupReport(user);
        return true;
    }

    public void sendActivationEmail(User user) {
        var issued = tokenService.issueAccountActivation(user);
        accountEmailService.sendAccountActivation(user, issued.rawToken());
    }
}
