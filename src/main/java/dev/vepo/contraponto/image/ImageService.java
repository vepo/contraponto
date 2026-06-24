package dev.vepo.contraponto.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    public static String contentTypeForExtension(String extensionWithDot) {
        if (extensionWithDot == null || extensionWithDot.isBlank()) {
            return "image/png";
        }
        return switch (extensionWithDot.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".avif" -> "image/avif";
            case ".svg" -> "image/svg+xml";
            default -> "image/png";
        };
    }

    private static String contentTypeForFilename(String filename, String storedContentType) {
        String fromExtension = contentTypeForExtension(getFileExtensionStatic(filename));
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (storedContentType != null && isKnownImageContentType(storedContentType)) {
            return storedContentType;
        }
        return fromExtension;
    }

    private static String getFileExtensionStatic(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return "";
    }

    private static boolean isKnownImageContentType(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/avif") ||
                contentType.equals("image/svg+xml");
    }

    private final ImageRepository imageRepository;

    private final ImageContentRepository imageContentRepository;

    private final ImageDependencyRepository dependencyRepository;

    private final ImageResizeService imageResizeService;

    @Inject
    public ImageService(ImageRepository imageRepository,
                        ImageContentRepository imageContentRepository,
                        ImageDependencyRepository dependencyRepository,
                        ImageResizeService imageResizeService) {
        this.imageRepository = imageRepository;
        this.imageContentRepository = imageContentRepository;
        this.dependencyRepository = dependencyRepository;
        this.imageResizeService = imageResizeService;
    }

    @Transactional
    public void deleteImage(String uuid, long ownerId) {
        Image image = imageRepository.findByUuidAndOwnerId(uuid, ownerId)
                                     .orElseThrow(() -> new WebApplicationException("Image not found",
                                                                                    Response.Status.NOT_FOUND));

        if (dependencyRepository.isReferenced(image.getId())) {
            throw new WebApplicationException("Image is in use and cannot be deleted",
                                              Response.Status.CONFLICT);
        }

        imageContentRepository.deleteByImageId(image.getId());
        imageRepository.softDelete(uuid);
        logger.info("Image deleted successfully! {}", image);
    }

    private String getFileExtension(String filename) {
        return getFileExtensionStatic(filename);
    }

    public ImageData getImage(String filename) {
        return getImage(filename, null);
    }

    public ImageData getImage(String filename, Integer maxWidth) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0) {
            throw new WebApplicationException("Image not found", Response.Status.NOT_FOUND);
        }

        String uuid = filename.substring(0, lastDot);
        Image image = imageRepository.findByUuid(uuid)
                                     .orElseThrow(() -> new WebApplicationException("Image not found",
                                                                                    Response.Status.NOT_FOUND));

        byte[] data = imageContentRepository.findContentByImageId(image.getId())
                                            .orElseThrow(() -> new WebApplicationException("Image content not found",
                                                                                           Response.Status.NOT_FOUND));
        var original = new ImageData(data, contentTypeForFilename(filename, image.getContentType()), image.getSize());
        if (maxWidth == null || maxWidth <= 0) {
            return original;
        }
        return imageResizeService.resize(original, maxWidth, filename);
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && isKnownImageContentType(contentType);
    }

    @Transactional
    public Image storeImportedImage(Blog blog,
                                    String uuid,
                                    String ext,
                                    byte[] content,
                                    String contentType,
                                    String gitAssetRelativePath) {
        validateImage(contentType, content.length);
        String filename = uuid + ext;
        User owner = blog.getOwner();
        var image = new Image(uuid,
                              filename,
                              contentType,
                              (long) content.length,
                              "/api/images/%s".formatted(filename),
                              owner);
        if (gitAssetRelativePath != null && !gitAssetRelativePath.isBlank()) {
            image.setGitAssetRelativePath(gitAssetRelativePath);
        }
        if (owner != null) {
            image.setUploadedBy(owner);
        }
        imageRepository.save(image);
        imageContentRepository.save(image, content);
        return image;
    }

    @Transactional
    public ImageResponse uploadImage(String filename,
                                     String contentType,
                                     InputStream data,
                                     long size,
                                     User owner,
                                     User uploadedBy) {
        try {
            validateImage(contentType, size);

            byte[] content = data.readAllBytes();
            if (content.length != size) {
                size = content.length;
            }
            validateImage(contentType, size);

            var extension = getFileExtension(filename);
            var imageIdentifier = UUID.randomUUID().toString();
            var uniqueFilename = imageIdentifier + extension;
            var url = "/api/images/%s".formatted(uniqueFilename);

            var image = new Image(imageIdentifier,
                                  uniqueFilename,
                                  contentType,
                                  size,
                                  url,
                                  owner);
            image.setUploadedBy(uploadedBy != null ? uploadedBy : owner);
            imageRepository.save(image);
            imageContentRepository.save(image, content);

            logger.info("Image uploaded successfully: {} -> {}", filename, url);

            return new ImageResponse(image.getUuid(),
                                     url,
                                     image.getFilename(),
                                     image.getContentType(),
                                     image.getSize(),
                                     image.getAltText());
        } catch (IOException e) {
            logger.error("Failed to upload image: {}", filename, e);
            throw new WebApplicationException("Failed to upload image", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateImage(String contentType, long size) {
        if (!isValidImageType(contentType)) {
            throw new WebApplicationException("Invalid image type. Only JPEG, PNG, GIF, WebP, AVIF, and SVG are allowed.",
                                              Response.Status.BAD_REQUEST);
        }
        if (size > MAX_SIZE_BYTES) {
            throw new WebApplicationException("File too large. Maximum size is 10MB.",
                                              Response.Status.BAD_REQUEST);
        }
    }
}
