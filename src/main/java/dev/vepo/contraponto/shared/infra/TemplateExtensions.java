package dev.vepo.contraponto.shared.infra;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {

    @TemplateExtension
    public static String or(UserContext.UserInfo user, Object defaultValue) {
        return user != null ? user.getName() : String.valueOf(defaultValue);
    }

    @TemplateExtension
    public static boolean isAuthenticated(UserContext.UserInfo user) {
        return user != null && user.isAuthenticated();
    }

    @TemplateExtension
    public static String avatarUrl(UserContext.UserInfo user) {
        if (user == null)
            return "";
        return user.getAvatarUrl();
    }

    @TemplateExtension
    public static String initials(UserContext.UserInfo user) {
        if (user == null)
            return "";
        return user.getInitials();
    }

    @TemplateExtension
    public static String firstName(UserContext.UserInfo user) {
        if (user == null)
            return "";
        return user.getFirstName();
    }
}