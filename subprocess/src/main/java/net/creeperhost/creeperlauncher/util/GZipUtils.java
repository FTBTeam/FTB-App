package net.creeperhost.creeperlauncher.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static void createTarBall(Path target, Path output, Set<String> rootPaths) throws IOException {
        try (
            var outputFilesStream = new FileOutputStream(output.toFile());
            var bufferOutSteam = new BufferedOutputStream(outputFilesStream);
            var gzipOutSteam = new GZIPOutputStream(bufferOutSteam);
            var tarOutSteam = new TarArchiveOutputStream(gzipOutSteam);
        ) {
            tarOutSteam.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            tarOutSteam.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            
            Files.walkFileTree(target, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    var relativePath = target.relativize(dir);
                    if (isInvalidDir(relativePath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    var tarEntry = new TarArchiveEntry(dir.toFile(), relativePath.toString());
                    tarOutSteam.putArchiveEntry(tarEntry);
                    tarOutSteam.closeArchiveEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isSymbolicLink()) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    var relativePath = target.relativize(file);
                    
                    // If the paths to include is empty, include them all, otherwise verify we're on the base path and 
                    // skip over that directory 
                    if (isInvalidDir(relativePath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    try {
                        var tarEntry = new TarArchiveEntry(file.toFile(), relativePath.toString());
                        tarOutSteam.putArchiveEntry(tarEntry);
                        Files.copy(file, tarOutSteam);
                        tarOutSteam.closeArchiveEntry();
                    } catch (IOException e) {
                        LOGGER.warn("Unable to create tar entry for {}", file.toString());
                    }

                    return FileVisitResult.CONTINUE;
                }
                
                private boolean isInvalidDir(Path path) {
                    if (rootPaths.size() == 0) {
                        return false;
                    }
                    
                    // If the paths to include is empty, include them all, otherwise verify we're on the base path and 
                    // skip over that directory
                    return rootPaths.stream().noneMatch(e -> path.startsWith(e + "/"));
                }
            });

            tarOutSteam.finish();
        }
    }

    public static void decompressTarBall(Path filePath, Path dir) throws IOException {
        try(
            var fileInputStream = Files.newInputStream(filePath);
            var bufferInputStream = new BufferedInputStream(fileInputStream);
            var gzipInputStream = new GZIPInputStream(bufferInputStream);
            var tarInputStream = new TarArchiveInputStream(gzipInputStream);
        ) {
            ArchiveEntry entry;
            while ((entry = tarInputStream.getNextEntry()) != null) {
                var path = dir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    if (Files.notExists(path)) {
                        Files.createDirectories(path);
                    }
                } else {
                    Files.copy(tarInputStream, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
