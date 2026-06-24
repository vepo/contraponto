package dev.vepo.contraponto.image;

import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImageAccess {

    public boolean canManage(Image image, LoggedUser user) {
        if (image == null || user == null || image.getOwner() == null) {
            return false;
        }
        return image.getOwner().getId().equals(user.getId());
    }
}
