package dev.vepo.contraponto.activitypub.actor;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import dev.vepo.contraponto.activitypub.ActivityPubSettings;

@ApplicationScoped
public class ActivityPubKeyPairService {

    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int RSA_KEY_SIZE = 2048;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final ActivityPubSettings settings;

    @Inject
    public ActivityPubKeyPairService(ActivityPubSettings settings) {
        this.settings = settings;
    }

    public PrivateKey decryptPrivateKey(String encryptedPrivateKey) {
        var secret = settings.keyEncryptionSecret()
                             .orElseThrow(() -> new BadRequestException("ActivityPub key encryption secret is not configured"));
        try {
            var combined = Base64.getDecoder().decode(encryptedPrivateKey);
            var iv = new byte[GCM_IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_BYTES);
            var ciphertext = new byte[combined.length - GCM_IV_BYTES];
            System.arraycopy(combined, GCM_IV_BYTES, ciphertext, 0, ciphertext.length);
            var keyBytes = deriveAesKey(secret);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyBytes, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var decoded = cipher.doFinal(ciphertext);
            var spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to decrypt private key", ex);
        }
    }

    private SecretKey deriveAesKey(String secret) {
        var digest = sha256(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, 0, AES_KEY_BYTES, "AES");
    }

    public String encryptPrivateKey(PrivateKey privateKey) {
        var secret = settings.keyEncryptionSecret()
                             .orElseThrow(() -> new BadRequestException("ActivityPub key encryption secret is not configured"));
        try {
            var keyBytes = deriveAesKey(secret);
            var iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyBytes, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var encrypted = cipher.doFinal(privateKey.getEncoded());
            var combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt private key", ex);
        }
    }

    public KeyPair generateRsaKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to generate RSA key pair", ex);
        }
    }

    public PublicKey parsePublicKeyPem(String pem) {
        try {
            var normalized = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "");
            var spec = new X509EncodedKeySpec(Base64.getDecoder().decode(normalized));
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Invalid public key PEM", ex);
        }
    }

    private byte[] sha256(byte[] input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public String toPublicKeyPem(PublicKey publicKey) {
        var encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----".formatted(encoded);
    }
}
