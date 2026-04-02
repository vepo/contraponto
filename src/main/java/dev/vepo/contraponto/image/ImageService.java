package dev.vepo.contraponto.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @ConfigProperty(name = "image.storage.path", defaultValue = "/tmp/contraponto-images")
    String storagePath;

    @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080")
    String baseUrl;

    @Inject
    ImageRepository imageRepository;

    @Transactional
    public ImageResponse uploadImage(String filename, String contentType, InputStream data, long size) {
        try {
            // Validate file type
            if (!isValidImageType(contentType)) {
                throw new WebApplicationException("Invalid image type. Only JPEG, PNG, GIF, WebP are allowed.",
                                                  Response.Status.BAD_REQUEST);
            }

            // Validate file size (max 10MB)
            if (size > 10 * 1024 * 1024) {
                throw new WebApplicationException("File too large. Maximum size is 10MB.",
                                                  Response.Status.BAD_REQUEST);
            }

            // Create storage directory if not exists
            Path uploadPath = Paths.get(storagePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String extension = getFileExtension(filename);
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Save file
            Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);

            // Generate URL
            String url = baseUrl + "/api/images/" + uniqueFilename;

            // Create image entity
            Image image = new Image(filename, contentType, size, filePath.toString(), url);
            imageRepository.save(image);

            logger.info("Image uploaded successfully: {} -> {}", filename, url);

            return new ImageResponse(image.getUuid(), url, image.getFilename(), image.getContentType(), image.getSize());
        } catch (IOException e) {
            logger.error("Failed to upload image: {}", filename, e);
            throw new WebApplicationException("Failed to upload image", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public ImageData getImage(String filename) throws IOException {
        Path filePath = Paths.get(storagePath).resolve(filename);

        if (!Files.exists(filePath)) {
            throw new WebApplicationException("Image not found", Response.Status.NOT_FOUND);
        }

        // Find image metadata in database
        String uuid = filename.substring(0, filename.lastIndexOf('.'));
        Image image = imageRepository.findByUuid(uuid)
                                     .orElseThrow(() -> new WebApplicationException("Image metadata not found",
                                                                                    Response.Status.NOT_FOUND));

        byte[] data = Files.readAllBytes(filePath);
        return new ImageData(data, image.getContentType(), image.getSize());
    }

    @Transactional
    public void deleteImage(String uuid) {
        Image image = imageRepository.findByUuid(uuid)
                                     .orElseThrow(() -> new WebApplicationException("Image not found",
                                                                                    Response.Status.NOT_FOUND));

        // Delete file from disk
        try {
            Path filePath = Paths.get(image.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.warn("Failed to delete image file: {}", image.getFilePath(), e);
        }

        // Soft delete from database
        imageRepository.softDelete(uuid);

        logger.info("Image deleted successfully: {}", uuid);
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp"));
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return "";
    }
}