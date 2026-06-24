package dev.vepo.contraponto.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ImageResizeServiceTest {

    private static byte[] encodePng(BufferedImage image) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static BufferedImage solidImage(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private final ImageResizeService resizeService = new ImageResizeService();

    @Test
    void returnsOriginalWhenImageAlreadyNarrow() throws Exception {
        var original = encodePng(solidImage(400, 200));
        var data = new ImageData(original, "image/png", original.length);

        var resized = resizeService.resize(data, 560, "abc.png");

        assertThat(resized.data()).isEqualTo(original);
    }

    @Test
    void scalesWideImagesDown() throws Exception {
        var original = encodePng(solidImage(1200, 600));
        var data = new ImageData(original, "image/png", original.length);

        var resized = resizeService.resize(data, 560, "abc.png");

        assertThat(resized.data().length).isLessThan(original.length);
        var image = ImageIO.read(new java.io.ByteArrayInputStream(resized.data()));
        assertThat(image.getWidth()).isEqualTo(560);
    }
}
