package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.user.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PasswordRecoveryService {

    public record ResetResult(boolean success, boolean invalidToken, String errorMessage) {
        static ResetResult succeeded() {
            return new ResetResult(true, false, null);
        }

        static ResetResult tokenInvalid() {
            return new ResetResult(false, true, null);
        }

        static ResetResult failed(String message) {
            return new ResetResult(false, false, message);
        }
    }

    private final UserRepository userRepository;
    private final UserAccountTokenService tokenService;
    private final AccountEmailService accountEmailService;
    private final PasswordService passwordService;

    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public PasswordRecoveryService(UserRepository userRepository,
                                   UserAccountTokenService tokenService,
                                   AccountEmailService accountEmailService,
                                   PasswordService passwordService,
                                   LoggedUserProvider loggedUserProvider) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.accountEmailService = accountEmailService;
        this.passwordService = passwordService;
        this.loggedUserProvider = loggedUserProvider;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByEmail(email.trim())
                      .filter(User::isActive)
                      .ifPresent(user -> {
                          var issued = tokenService.issuePasswordReset(user);
                          accountEmailService.sendPasswordReset(user, issued.rawToken());
                      });
    }

    @Transactional
    public ResetResult resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            return ResetResult.failed("Password is required.");
        }
        if (newPassword.length() < 8) {
            return ResetResult.failed("Password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResetResult.failed("Passwords do not match.");
        }

        var maybeToken = tokenService.consume(rawToken);
        if (maybeToken.isEmpty()) {
            return ResetResult.tokenInvalid();
        }

        var token = maybeToken.get();
        var user = token.getUser();
        if (!user.isActive()) {
            return ResetResult.tokenInvalid();
        }

        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.update(user);
        loggedUserProvider.invalidateAllSessionsForUser(user.getId());
        accountEmailService.sendPasswordChanged(user);

        return ResetResult.succeeded();
    }
}
