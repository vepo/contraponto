package dev.vepo.contraponto.reddit.account;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import dev.vepo.contraponto.reddit.RedditSettings;

/**
 * Encrypts the Reddit OAuth refresh token at rest. Deliberately a separate
 * class and a separate config secret from {@code ActivityPubKeyPairService} —
 * per ADR-0018, integrations must not share an encryption secret so a rotation
 * or compromise in one cannot silently affect another.
 */
@ApplicationScoped
public class RedditTokenCipherService {

    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final RedditSettings settings;

    @Inject
    public RedditTokenCipherService(RedditSettings settings) {
        this.settings = settings;
    }

    public String decrypt(String encryptedRefreshToken) {
        var secret = requiredSecret();
        try {
            var combined = Base64.getDecoder().decode(encryptedRefreshToken);
            var iv = new byte[GCM_IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_BYTES);
            var ciphertext = new byte[combined.length - GCM_IV_BYTES];
            System.arraycopy(combined, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);
            var keyBytes = deriveAesKey(secret);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyBytes, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to decrypt Reddit refresh token", ex);
        }
    }

    private SecretKey deriveAesKey(String secret) {
        var digest = sha256(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, 0, AES_KEY_BYTES, "AES");
    }

    public String encrypt(String refreshToken) {
        var secret = requiredSecret();
        try {
            var keyBytes = deriveAesKey(secret);
            var iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyBytes, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var encrypted = cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            var combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt Reddit refresh token", ex);
        }
    }

    private String requiredSecret() {
        return settings.tokenEncryptionSecret()
                       .orElseThrow(() -> new BadRequestException("Reddit token encryption secret is not configured"));
    }

    private byte[] sha256(byte[] input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
