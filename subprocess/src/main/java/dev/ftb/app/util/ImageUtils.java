package dev.ftb.app.util;

import javax.annotation.WillNotClose;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static BufferedImage resizeImage(@WillNotClose InputStream originalImage, int targetWidth, int targetHeight) throws IOException {
        return resizeImage(ImageIO.read(originalImage), targetWidth, targetHeight);
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
}
