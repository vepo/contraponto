package dev.vepo.contraponto.user;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class UserTemplateExtensions {

    @TemplateExtension
    public static String avatarUrl(LoggedUser user) {
        return AvatarUrls.avatarUrl(user);
    }

    @TemplateExtension
    public static String avatarUrl(User user) {
        return AvatarUrls.avatarUrl(user);
    }

    @TemplateExtension
    public static String firstName(LoggedUser user) {
        if (user == null) {
            return "";
        }
        return user.getFirstName();
    }

    private UserTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
