package dev.ftb.app.util;

import net.covers1624.quack.util.SneakyUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static net.covers1624.quack.util.SneakyUtils.sneak;

public class FileUtils
{
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Allows the subprocess to open a desktop's finder / explorer / etc
     *
     * @param location the path to open                
     * @return if the open action was successful
     */
    public static boolean openFolder(Path location) {
        if (Files.notExists(location)) {
            return false;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(location.toFile());
            } catch (IOException ignored) {}
            return true;
        }
        return false;
    }

    
    public static void deletePath(Path path) {
        if (Files.notExists(path)) return;

        if (Files.isDirectory(path)) {
            deleteDirectory(path);
        } else {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {}
        }
    }

    public static void deleteDirectory(Path file) {
        if (Files.notExists(file)) return;

        try (Stream<Path> files = Files.walk(file)) {
                    files
                    .filter(Files::exists)
                    .sorted(Comparator.reverseOrder())
                    .forEach(e -> {
                        try {
                            Files.deleteIfExists(e);
                        } catch (IOException ex) {
                            if (Files.exists(e)) {
                                SneakyUtils.throwUnchecked(ex);
                            }
                        }
                    });

        } catch (IOException e) {
            LOGGER.error("Failed to delete directory. {}", file, e);
        }
    }

    @Deprecated//Use UnixPlatform#chmod755
    public static void setFilePermissions(Path file)
    {
        try
        {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //I hate this but its the only way I can get it to work right now
    public static boolean removeMeta(Path file)
    {
        if(!Files.exists(file)) return false;
        try (FileSystem fileSystem = FileSystems.newFileSystem(file))
        {
            Path root = fileSystem.getPath("/");
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        if (file.startsWith("/META-INF")) {
                            Files.deleteIfExists(file);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to delete entry from META-INF", e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

//            FileUtils.deleteDirectory(meta);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to remove meta from {}", file, e);
        }
        return false;
    }

    public static boolean mergeJars(Path input, Path output) {
        if(input == null || output == null) return false;
        AtomicBoolean flag = new AtomicBoolean(true);

        try (FileSystem fs = FileSystems.newFileSystem(output)) {
            Path dstRoot = fs.getPath("/");
            try (FileSystem srcFs = FileSystems.newFileSystem(input)) {
                Path srcRoot = srcFs.getPath("/");
                Files.walkFileTree(srcRoot, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path dst = dstRoot.resolve(srcRoot.relativize(file));
                        try {
                            Files.createDirectories(dst.getParent());
                            Files.copy(file, dst, REPLACE_EXISTING);
                        } catch (Exception e) {
                            flag.set(false);
                            e.printStackTrace();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (Exception e) {
            flag.set(false);
            e.printStackTrace();
        }
        return flag.get();
    }

    public static Path createDirectories(Path dir)
    {
        try {
            return Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories.", e);
        }
        return null;
    }

    public static List<Path> listDir(Path dir) {
        if (Files.notExists(dir)) return Collections.emptyList();

        try (Stream<Path> stream = Files.walk(dir, 1)) {
            return stream.filter(e -> !e.equals(dir))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Failed to list directory. {}", dir.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    @Deprecated// Use move(Path, Path, boolean)
    private static HashMap<Pair<Path, Path>, IOException> moveDirectory(Path in, Path out, boolean replaceExisting, boolean failFast) {
        HashMap<Pair<Path, Path>, IOException> errors = new HashMap<>();
        if (!in.getFileName().toString().equals(out.getFileName().toString()))
        {
            out = out.resolve(in.getFileName().toString());
        }
        if (replaceExisting && Files.exists(out))
        {
            if (Files.isDirectory(out))
            {
                FileUtils.deleteDirectory(out);
            } else {
                try {
                    Files.deleteIfExists(out);
                } catch (IOException e) {
                    // shrug
                }
            }
        }

        try {
            Files.move(in, out);
            return errors;
        } catch (IOException e) {
            LOGGER.warn("Could not move {} to {} - trying another method", in, out, e);
        }

        try {
            Path finalOut = out;
            Files.walkFileTree(in, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {

                    Path relative = in.getParent().relativize(path);
                    if (in.getFileName().toString().equals(finalOut.getFileName().toString())) {
                        relative = in.relativize(path);
                    }
                    Path outFile = finalOut.resolve(relative);
                    Files.createDirectories(outFile.getParent());
                    try {
                        Files.move(path, outFile);
                    } catch (IOException e) {
                        boolean copyFailed = true;
                        try {
                            Files.copy(path, outFile); // try and copy anyway
                            copyFailed = false;
                        } catch (IOException e2) {
                            errors.put(Pair.of(path, outFile), e2);
                            if (failFast)
                                return FileVisitResult.TERMINATE;
                        }

                        if (!copyFailed)
                        {
                            try {
                                Files.delete(path); // try to delete even if we couldn't move, but if we could copy
                            } catch (Exception ignored) {
                                // shrug
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException
                {
                    List<Path> list = FileUtils.listDir(directory);
                    if (list.isEmpty())
                    {
                        try {
                            Files.delete(directory);
                        } catch (Exception ignored) {
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return errors;
    }

    @Deprecated// Use move(Path, Path, boolean)
    public static HashMap<Pair<Path, Path>, IOException> move(Path in, Path out)
    {
        return move(in, out, false, true);
    }

    @Deprecated// Use move(Path, Path, boolean)
    public static HashMap<Pair<Path, Path>, IOException> move(Path in, Path out, boolean replaceExisting, boolean failFast)
    {
        if (Files.isDirectory(in))
        {
            return moveDirectory(in, out, replaceExisting, failFast);
        }
        HashMap<Pair<Path, Path>, IOException> errors = new HashMap<>();
        try
        {
            if (Files.exists(out) && Files.isDirectory(out))
            {
                out = out.resolve(in.getFileName().toString());
            }
            if (replaceExisting) {
                Files.move(in, out, REPLACE_EXISTING);
            } else {
                Files.move(in, out);
            }
        } catch (IOException e) {
            errors.put(Pair.of(in, out), e);
        }
        return errors;
    }

    public static void move(Path from, Path to, boolean replaceExisting) throws IOException {
        if (Files.isDirectory(from)) {
            if (!Files.isDirectory(to)) throw new IllegalArgumentException("Requested to move directory into file.");
            try (Stream<Path> children = Files.list(from)) {
                children.filter(e -> e != from)
                        .forEach(sneak(e -> {
                            move(e, to.resolve(e.getFileName()), replaceExisting);
                        }));

            }
        } else {
            if (Files.exists(from)) {
                Files.createDirectories(from.toAbsolutePath().getParent());
                if(replaceExisting) {
                    Files.move(from, to, REPLACE_EXISTING);
                } else {
                    Files.move(from, to);
                }
            }
        }
    }

    /**
     * Sanitize filename or path segment name to not include problematic
     * characters for some filesystems (eg: Windows)
     *
     * @param name the name to sanitize
     * @return the name with problematic characters replaced with {@code _} (underscore)
     */
    public static String stripInvalidChars(String name) {
        final String reserved = "<>:\"|?*";
        StringBuilder result = new StringBuilder();
        for (char c: name.toCharArray()) {
            if (Character.isISOControl(c) || reserved.indexOf(c) >= 0) {
                result.append('_'); // replacement char
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String getAcl(Path path) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ACL info for: ").append(path).append("\n");
            AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
            for (AclEntry entry : view.getAcl()) {
                sb.append(entry.principal().getName()).append("\n");
                sb.append(" === flags ===").append("\n");
                for (AclEntryFlag flags : entry.flags()) {
                    sb.append("  ").append(flags.name()).append("\n");
                }

                sb.append(" === permissions ===").append("\n");
                for (AclEntryPermission permission : entry.permissions()) {
                    sb.append("  ").append(permission.name()).append("\n");
                }
            }
            return sb.toString();
        } catch (Throwable ex) {
            return "Failed to get ACL.\n" + ExceptionUtils.getMessage(ex);
        }
    }

    /**
     * Checks if a give path is writable by the current process (This app)
     * 
     * @param path The path to check
     * @return If the path is writable by the current process.
     */
    public static boolean pathWritableByApp(Path path) {
        try {
            var testFile = path.resolve(".ftb-app-write-test-file");
            Files.createFile(testFile);
            Files.delete(testFile);
            return true;
        } catch (Throwable ex) {
            LOGGER.error("Failed to check if path is writable by app.", ex);
            return false;
        }
    }
}
