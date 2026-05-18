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
public class EmailVerificationService {

    private final UserAccountTokenService tokenService;
    private final UserRepository userRepository;
    private final AccountEmailService accountEmailService;
    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public EmailVerificationService(UserAccountTokenService tokenService,
                                    UserRepository userRepository,
                                    AccountEmailService accountEmailService,
                                    LoggedUserProvider loggedUserProvider) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.accountEmailService = accountEmailService;
        this.loggedUserProvider = loggedUserProvider;
    }

    @Transactional
    public void requestEmailChange(User user, String newEmail) {
        user.setPendingEmail(newEmail);
        userRepository.update(user);
        var issued = tokenService.issueEmailChange(user, newEmail);
        accountEmailService.sendEmailChangeVerification(user, newEmail, issued.rawToken());
    }

    @Transactional
    public Optional<String> verify(String rawToken, LoggedUser loggedUser) {
        var maybeToken = tokenService.consume(rawToken);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }

        var token = maybeToken.get();
        if (token.getType() != UserAccountTokenType.EMAIL_CHANGE) {
            return Optional.empty();
        }

        var user = token.getUser();
        String newEmail = token.getNewEmail();
        if (newEmail == null || newEmail.isBlank()) {
            return Optional.empty();
        }
        if (!newEmail.equals(user.getPendingEmail())) {
            return Optional.empty();
        }
        if (userRepository.existsByEmail(newEmail, user.getId())) {
            return Optional.empty();
        }

        String previousEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setPendingEmail(null);
        userRepository.update(user);

        if (loggedUser.isAuthenticated() && loggedUser.getId() == user.getId()) {
            loggedUserProvider.update(loggedUser.getSessionId(), user);
        }

        accountEmailService.sendEmailChangedNotice(previousEmail, newEmail);
        return Optional.of(newEmail);
    }
}
