package dev.vepo.contraponto.blog;

import java.util.Optional;

import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogBannerService {

    private final ImageRepository imageRepository;

    @Inject
    public BlogBannerService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public void applyBannerFromForm(Blog blog, String bannerId) {
        if (bannerId == null) {
            return;
        }
        if (bannerId.isBlank()) {
            blog.setBanner(null);
            return;
        }
        resolveImageForOwner(blog.getOwner().getId(), bannerId).ifPresent(blog::setBanner);
    }

    public void applyDefaultBannerOnCreate(Blog blog) {
        if (blog.getBanner() != null) {
            return;
        }
        User owner = blog.getOwner();
        if (owner == null || owner.getDefaultBlogBanner() == null) {
            return;
        }
        blog.setBanner(owner.getDefaultBlogBanner());
    }

    public void applyDefaultBlogBanner(User user, String defaultBannerId) {
        if (defaultBannerId == null) {
            return;
        }
        if (defaultBannerId.isBlank()) {
            user.setDefaultBlogBanner(null);
            return;
        }
        resolveImageForOwner(user.getId(), defaultBannerId).ifPresent(user::setDefaultBlogBanner);
    }

    public void applyProfilePicture(User user, String profilePictureId) {
        if (profilePictureId == null) {
            return;
        }
        if (profilePictureId.isBlank()) {
            user.setProfilePicture(null);
            return;
        }
        resolveImageForOwner(user.getId(), profilePictureId).ifPresent(user::setProfilePicture);
    }

    public String effectiveBannerUrl(Blog blog) {
        return resolveEffectiveBanner(blog).map(Image::getUrl).orElse(null);
    }

    public Optional<Image> resolveEffectiveBanner(Blog blog) {
        if (blog == null) {
            return Optional.empty();
        }
        if (blog.getBanner() != null) {
            return Optional.of(blog.getBanner());
        }
        User owner = blog.getOwner();
        if (owner != null && owner.getDefaultBlogBanner() != null) {
            return Optional.of(owner.getDefaultBlogBanner());
        }
        return Optional.empty();
    }

    public Optional<Image> resolveImageForOwner(long ownerId, String imageUuid) {
        if (imageUuid == null || imageUuid.isBlank()) {
            return Optional.empty();
        }
        return imageRepository.findByUuidAndOwnerId(imageUuid.trim(), ownerId);
    }
}
