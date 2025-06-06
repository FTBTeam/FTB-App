package dev.ftb.app.pack;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class InstanceArtwork {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceArtwork.class);
    
    private final Map<Byte, String> imageTypeMap = Map.of(
        (byte) 0x00, "png",
        (byte) 0x01, "jpg",
        (byte) 0x02, "gif",
        (byte) 0x03, "webp"
    );
    
    private final String name;
    private final Path imagePath;

    public InstanceArtwork(String name, Path instanceMetaPath) {
        this.name = name;
        this.imagePath = instanceMetaPath.resolve(name);
    }
    
    public void saveImage(InputStream sourceImg, String type) {
        var innerType = type.equals("jpeg") ? "jpg" : type;
        var imageType = resolveTypeFromExtension(innerType);
        
        if (imageType == -1) {
            LOGGER.error("Unknown image type: {}", type);
            return;
        }
        
        try {
            if (Files.notExists(imagePath.getParent())) {
                Files.createDirectories(imagePath.getParent());
            }
            
            // Save the image to the file
            try (InputStream inputStream = sourceImg;
                 OutputStream outputStream = Files.newOutputStream(imagePath)) {
                // Lazily copy of the bytes after writing the first byte
                outputStream.write(imageType);
                var allBytes = inputStream.readAllBytes();
                if (allBytes.length > 0) {
                    outputStream.write(allBytes);
                }
                // Flush the output stream to ensure all data is written
                outputStream.flush();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save image", e);
        }
    }
    
    @Nullable
    public String getBase64Image() {
        if (!exists()) {
            return null;
        }
        
        // Read the image as a string
        try (InputStream inputStream = Files.newInputStream(imagePath)) {
            // Read the first byte to determine the image type
            byte[] firstByte = new byte[1];
            
            // Read the first byte
            if (inputStream.read(firstByte) != 1) {
                return null;
            }
            
            // Determine the image type
            String imageType = imageTypeMap.get(firstByte[0]);
            if (imageType == null) {
                return null;
            }
            
            // Read the rest of the image
            byte[] imageBytes = inputStream.readAllBytes();
            if (imageBytes.length == 0) {
                return null;
            }
            
            // Convert the image bytes to a Base64 string
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/" + imageType + ";base64," + base64Image;
        } catch (Exception e) {
            
            return null;
        }
    }
    
    private byte resolveTypeFromExtension(String ext) {
        for (Map.Entry<Byte, String> entry : imageTypeMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(ext)) {
                return entry.getKey();
            }
        }
        
        return -1;
    }
    
    public boolean exists() {
        return Files.exists(imagePath);
    }

    public String name() {
        return name;
    }
}
