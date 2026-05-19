package dev.vepo.contraponto.git;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Maps Git/Jekyll asset file names to {@code tb_images.uuid} (VARCHAR 36).
 * Contraponto-native exports use canonical UUIDs; legacy Jekyll repos often use
 * longer slugs.
 */
final class GitImportedAssetId {

    private static final Pattern CANONICAL_UUID = Pattern.compile(
                                                                  "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    static String normalize(String basenameWithoutExtension, String extensionWithDot) {
        String key = basenameWithoutExtension.strip();
        if (CANONICAL_UUID.matcher(key).matches()) {
            return key.toLowerCase(Locale.ROOT);
        }
        String ext = extensionWithDot == null ? "" : extensionWithDot.toLowerCase(Locale.ROOT);
        String material = "contraponto-git-asset:" + key + ext;
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private GitImportedAssetId() {}
}
