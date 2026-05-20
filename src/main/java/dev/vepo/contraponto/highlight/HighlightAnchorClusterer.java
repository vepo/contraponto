package dev.vepo.contraponto.highlight;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HighlightAnchorClusterer {

    public String clusterHash(String passage) {
        String normalized = normalizePassage(passage);
        return sha256(normalized);
    }

    public String normalizePassage(String passage) {
        if (passage == null) {
            return "";
        }
        return passage.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
