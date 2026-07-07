package dev.vepo.contraponto.activitypub;

final class ActivityPubActorUrls {

    static boolean actorUrisMatch(String left, String right) {
        return normalizeActorUri(left).equals(normalizeActorUri(right));
    }

    static String actorUrlFromKeyId(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            return "";
        }
        var hash = keyId.indexOf('#');
        return hash >= 0 ? keyId.substring(0, hash) : keyId;
    }

    static String normalizeActorUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        var trimmed = uri.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ActivityPubActorUrls() {}
}
