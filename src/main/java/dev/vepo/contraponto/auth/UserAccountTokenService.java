package dev.vepo.contraponto.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserAccountTokenService {

    public record IssuedToken(String rawToken, UserAccountToken token) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private final UserAccountTokenRepository tokenRepository;

    private final int passwordResetHours;

    private final int emailChangeHours;

    private final int accountActivationHours;

    @Inject
    public UserAccountTokenService(UserAccountTokenRepository tokenRepository,
                                   @ConfigProperty(name = "app.account-token.password-reset-hours", defaultValue = "1") int passwordResetHours,
                                   @ConfigProperty(name = "app.account-token.email-change-hours", defaultValue = "24") int emailChangeHours,
                                   @ConfigProperty(name = "app.account-token.account-activation-hours", defaultValue = "48") int accountActivationHours) {
        this.tokenRepository = tokenRepository;
        this.passwordResetHours = passwordResetHours;
        this.emailChangeHours = emailChangeHours;
        this.accountActivationHours = accountActivationHours;
    }

    @Transactional
    public Optional<UserAccountToken> consume(String rawToken) {
        var maybeToken = findValidToken(rawToken);
        if (maybeToken.isEmpty()) {
            return Optional.empty();
        }
        var token = maybeToken.get();
        tokenRepository.markUsed(token);
        return Optional.of(token);
    }

    public Optional<UserAccountToken> findValidToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findValidByHash(hashToken(rawToken));
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private IssuedToken issue(User user, UserAccountTokenType type, String newEmail, int validHours) {
        tokenRepository.invalidateActiveForUser(user, type);

        String rawToken = generateRawToken();
        var token = new UserAccountToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(hashToken(rawToken));
        token.setNewEmail(newEmail);
        token.setExpiresAt(LocalDateTime.now(ZoneId.systemDefault()).plusHours(validHours));
        tokenRepository.persist(token);

        return new IssuedToken(rawToken, token);
    }

    @Transactional
    public IssuedToken issueAccountActivation(User user) {
        return issue(user, UserAccountTokenType.ACCOUNT_ACTIVATION, null, accountActivationHours);
    }

    @Transactional
    public IssuedToken issueEmailChange(User user, String newEmail) {
        return issue(user, UserAccountTokenType.EMAIL_CHANGE, newEmail.trim(), emailChangeHours);
    }

    @Transactional
    public IssuedToken issuePasswordReset(User user) {
        return issue(user, UserAccountTokenType.PASSWORD_RESET, null, passwordResetHours);
    }
}
