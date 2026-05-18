package dev.vepo.contraponto.image;

import java.io.IOException;
import java.io.InputStream;
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

    private final ImageRepository imageRepository;
    private final ImageContentRepository imageContentRepository;
    private final ImageDependencyRepository dependencyRepository;

    @Inject
    public ImageService(ImageRepository imageRepository,
                        ImageContentRepository imageContentRepository,
                        ImageDependencyRepository dependencyRepository) {
        this.imageRepository = imageRepository;
        this.imageContentRepository = imageContentRepository;
        this.dependencyRepository = dependencyRepository;
    }

    @Transactional
    public void deleteImage(String uuid, long blogId) {
        Image image = imageRepository.findByUuidAndBlogId(uuid, blogId)
                                     .orElseThrow(() -> new WebApplicationException("Image not found",
                                                                                    Response.Status.NOT_FOUND));

        if (dependencyRepository.isReferenced(image.getId())) {
            throw new WebApplicationException("Image is in use and cannot be deleted",
                                              Response.Status.CONFLICT);
        }

        imageContentRepository.deleteByImageId(image.getId());
        imageRepository.softDelete(uuid);
        logger.info("Image deleted successfully! id={}", image.getId());
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return "";
    }

    public ImageData getImage(String filename) {
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
        return new ImageData(data, image.getContentType(), image.getSize());
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp"));
    }

    @Transactional
    public Image storeImportedImage(Blog blog, String uuid, String ext, byte[] content, String contentType) {
        validateImage(contentType, content.length);
        String filename = uuid + ext;
        var image = new Image(uuid,
                              filename,
                              contentType,
                              (long) content.length,
                              "/api/images/" + filename,
                              blog);
        imageRepository.save(image);
        imageContentRepository.save(image, content);
        return image;
    }

    @Transactional
    public ImageResponse uploadImage(String filename,
                                     String contentType,
                                     InputStream data,
                                     long size,
                                     Blog blog,
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
                                  blog);
            image.setUploadedBy(uploadedBy);
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
            throw new WebApplicationException("Invalid image type. Only JPEG, PNG, GIF, WebP are allowed.",
                                              Response.Status.BAD_REQUEST);
        }
        if (size > MAX_SIZE_BYTES) {
            throw new WebApplicationException("File too large. Maximum size is 10MB.",
                                              Response.Status.BAD_REQUEST);
        }
    }
}
