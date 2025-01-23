package dev.ftb.app.util;

import net.covers1624.quack.io.CopyingFileVisitor;
import net.covers1624.quack.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class ZipUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void createZipFromDirectory(Path target, Path output) throws IOException {
        createZipFromDirectory(target, output, Set.of());
    }

    public static void createZipFromDirectory(Path target, Path output, Set<String> rootPathWhitelist) throws IOException {
        if (Files.notExists(target)) {
            throw new IOException("%s does not exist!".formatted(target));
        }

        if (!Files.isDirectory(target)) {
            throw new IOException("%s is not a directory!".formatted(target));
        }

        try (FileSystem fs = IOUtils.getJarFileSystem(IOUtils.makeParents(output), true)) {
            Files.walkFileTree(target, new CopyingFileVisitor(target, fs.getPath("/"), path -> isWhitelistedRoot(path, rootPathWhitelist)));
        }
    }

    public static void extractZip(Path zipFile, Path dir) throws IOException {
        try (FileSystem fs = IOUtils.getJarFileSystem(zipFile, true)) {
            Files.walkFileTree(fs.getPath("/"), new CopyingFileVisitor(fs.getPath("/"), dir));
        }
    }

    /**
     * If the whitelist is empty, always allow the file, otherwise validate it's root folder is within
     * the whitelist supplied to the visitor
     *
     * @param path full path of the location
     * @return if the dir is allowed or not
     */
    private static boolean isWhitelistedRoot(Path path, Set<String> whitelist) {
        if (whitelist.size() == 0) { 
            return true;
        }
        
        if (path.toString().equals("")) {
            return true;
        }
        
        return whitelist.stream().anyMatch(path::startsWith);
    }
}
