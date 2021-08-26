package net.creeperhost.creeperlauncher.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageUtils {
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
    public static BufferedImage resizeImage(byte[] originalImage, int targetWidth, int targetHeight) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(originalImage);
        BufferedImage bi = ImageIO.read(bais);
        return resizeImage(bi, targetWidth, targetHeight);
    }
}
