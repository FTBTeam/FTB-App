package dev.ftb.app.util;

import com.google.common.util.concurrent.Striped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;

/// Crash-safe file writes.
///
/// A write goes to `name.tmp` next to the target, is flushed to disk, then atomically moved over the target. The
/// target is therefore always either the old or the new contents, never a partial write, even if the app crashes or
/// the disk fills up mid-write.
///
/// Writes to the same path are serialized, so concurrent callers can't clobber each other's temp file.
public final class SafeFiles {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final Striped<Lock> LOCKS = Striped.lazyWeakLock(64);

    private static final int MOVE_ATTEMPTS = 5;
    private static final long MOVE_RETRY_DELAY_MS = 50; // Potentially holds the main thread. Not ideal but likely important.

    private SafeFiles() {
    }
    
    public static Path tempPathFor(Path path) {
        return path.resolveSibling(path.getFileName() + ".tmp");
    }

    /// Writes content as UTF-8
    ///
    /// @see #writeAtomically(Path, byte[])
    public static void writeAtomically(Path path, String content) throws IOException {
        writeAtomically(path, content.getBytes(StandardCharsets.UTF_8));
    }

    /// Writes `bytes` to `path`, replacing any existing file, creating parent directories as needed.
    public static void writeAtomically(Path path, byte[] bytes) throws IOException {
        Path tmpPath = tempPathFor(path);
        Lock lock = LOCKS.get(path.toAbsolutePath().normalize());
        lock.lock();
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (FileChannel channel = FileChannel.open(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }

                channel.force(true); // Force the data to disk before we move it into place. This is important for crash-safety.
            } catch (AccessDeniedException ex) {
                LOGGER.warn("Failed to write {}, access denied. ACL: {}", tmpPath, FileUtils.getAcl(tmpPath.getParent()));
                throw ex;
            }

            try {
                moveWithRetry(tmpPath, path);
            } catch (IOException ex) {
                // Don't leave a temp file behind that a later load would have to clean up
                Files.deleteIfExists(tmpPath);
                throw ex;
            }
        } finally {
            lock.unlock();
        }
    }

    /// Moves `from` over `to`, atomically where the filesystem supports it.
    ///
    /// Retries when access is denied, which lets us get past anything holding a temporary lock on the file, such as
    /// an anti-virus or Windows indexing.
    public static void moveWithRetry(Path from, Path to) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                try {
                    Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AccessDeniedException ex) {
                if (attempt >= MOVE_ATTEMPTS) {
                    throw ex;
                }
                LOGGER.warn("Access denied moving {} to {}, retrying ({}/{})", from, to, attempt, MOVE_ATTEMPTS);
                try {
                    Thread.sleep(MOVE_RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }
}
