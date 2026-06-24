package dev.vepo.contraponto.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImageResizeService {

    private static String outputFormat(String contentType, String filename) {
        if (contentType != null && contentType.contains("png")) {
            return "png";
        }
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".png")) {
            return "png";
        }
        return "jpeg";
    }

    private static void writeJpeg(BufferedImage image, ByteArrayOutputStream output) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpeg", output);
            return;
        }
        var writer = writers.next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            var param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.85f);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private final ConcurrentHashMap<String, ImageData> cache = new ConcurrentHashMap<>();

    private ImageData doResize(ImageData original, int maxWidth, String filename, String contentType) {
        try {
            var input = ImageIO.read(new ByteArrayInputStream(original.data()));
            if (input == null || input.getWidth() <= maxWidth) {
                return original;
            }
            var scale = (double) maxWidth / input.getWidth();
            var targetHeight = Math.max(1, (int) Math.round(input.getHeight() * scale));
            var scaled = new BufferedImage(maxWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            var graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(input, 0, 0, maxWidth, targetHeight, null);
            graphics.dispose();

            var output = new ByteArrayOutputStream();
            var format = outputFormat(contentType, filename);
            if ("jpeg".equals(format)) {
                writeJpeg(scaled, output);
                return new ImageData(output.toByteArray(), "image/jpeg", output.size());
            }
            if (!ImageIO.write(scaled, format, output)) {
                return original;
            }
            var bytes = output.toByteArray();
            return new ImageData(bytes, ImageService.contentTypeForExtension("." + format), bytes.length);
        } catch (IOException | RuntimeException _) {
            return original;
        }
    }

    public ImageData resize(ImageData original, int maxWidth, String filename) {
        if (maxWidth <= 0 || original == null) {
            return original;
        }
        var contentType = original.contentType();
        if (contentType == null || contentType.contains("svg")) {
            return original;
        }
        var cacheKey = "%s:%d".formatted(filename, maxWidth);
        return cache.computeIfAbsent(cacheKey, key -> doResize(original, maxWidth, filename, contentType));
    }
}
