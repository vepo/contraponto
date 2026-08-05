package dev.vepo.contraponto.reddit.account;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import dev.vepo.contraponto.reddit.RedditSettings;

/**
 * CSRF-binds the OAuth {@code state} parameter to the current session without
 * server-side storage: {@code state} is an HMAC-SHA256 of the session id, keyed
 * by the Reddit token-encryption secret. The callback recomputes the HMAC for
 * the caller's current session and compares.
 */
@ApplicationScoped
public class RedditOAuthStateService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RedditSettings settings;

    @Inject
    public RedditOAuthStateService(RedditSettings settings) {
        this.settings = settings;
    }

    public String generate(String sessionId) {
        return hmac(sessionId);
    }

    private String hmac(String sessionId) {
        var secret = settings.tokenEncryptionSecret()
                             .orElseThrow(() -> new BadRequestException("Reddit token encryption secret is not configured"));
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            var digest = mac.doFinal(sessionId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to compute Reddit OAuth state", ex);
        }
    }

    public boolean verify(String sessionId, String state) {
        if (state == null || state.isBlank() || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(hmac(sessionId).getBytes(StandardCharsets.UTF_8),
                                     state.getBytes(StandardCharsets.UTF_8));
    }
}
