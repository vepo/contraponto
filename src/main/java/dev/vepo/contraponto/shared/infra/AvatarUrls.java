package dev.vepo.contraponto.shared.infra;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import dev.vepo.contraponto.user.User;

public final class AvatarUrls {

    public static String avatarUrl(LoggedUser loggedUser) {
        if (loggedUser == null) {
            return "";
        }
        var user = loggedUser.getUser();
        if (user != null) {
            return avatarUrl(user);
        }
        return generatedUrl(loggedUser.getName());
    }

    public static String avatarUrl(User user) {
        if (user == null) {
            return "";
        }
        if (user.getProfilePicture() != null) {
            return user.getProfilePicture().getUrl();
        }
        return generatedUrl(user.getName());
    }

    public static String generatedUrl(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        return "/components/avatar?name=%s".formatted(URLEncoder.encode(displayName.trim(), StandardCharsets.UTF_8));
    }

    private AvatarUrls() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
