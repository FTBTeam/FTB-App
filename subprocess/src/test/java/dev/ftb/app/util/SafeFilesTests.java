package dev.ftb.app.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SafeFilesTests {
    @TempDir
    Path dir;

    @Test
    public void testWritesNewFileAndCreatesParents() throws IOException {
        Path file = dir.resolve("a/b/file.txt");
        SafeFiles.writeAtomically(file, "hello");

        assertEquals("hello", Files.readString(file));
        assertFalse(Files.exists(SafeFiles.tempPathFor(file)));
    }

    @Test
    public void testReplacesExistingFile() throws IOException {
        Path file = dir.resolve("file.txt");
        Files.writeString(file, "a much longer original value");

        SafeFiles.writeAtomically(file, "new");

        assertEquals("new", Files.readString(file));
    }

    @Test
    public void testOverwritesLeftoverTempFile() throws IOException {
        Path file = dir.resolve("file.txt");
        Files.writeString(SafeFiles.tempPathFor(file), "partial junk from a crashed write that is longer");

        SafeFiles.writeAtomically(file, "good");

        assertEquals("good", Files.readString(file));
        assertFalse(Files.exists(SafeFiles.tempPathFor(file)));
    }

    @Test
    public void testFailedWriteLeavesOriginalUntouched() throws IOException {
        Path file = dir.resolve("file.txt");
        Files.writeString(file, "original");
        // A directory in the temp file's place makes the write fail before anything is moved
        Files.createDirectory(SafeFiles.tempPathFor(file));

        assertThrows(IOException.class, () -> SafeFiles.writeAtomically(file, "new"));
        assertEquals("original", Files.readString(file));
    }

    @Test
    public void testTempPathIsSiblingWithSuffix() {
        Path file = dir.resolve("data.json");
        assertEquals(dir.resolve("data.json.tmp"), SafeFiles.tempPathFor(file));
    }

    @Test
    public void testConcurrentWritesAlwaysLeaveACompleteFile() throws Exception {
        Path file = dir.resolve("file.txt");
        List<String> contents = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            contents.add(String.valueOf(i).repeat(1000));
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (String content : contents) {
                futures.add(executor.submit(() -> {
                    SafeFiles.writeAtomically(file, content);
                    return null;
                }));
            }
            for (var future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        assertTrue(contents.contains(Files.readString(file)));
        assertFalse(Files.exists(SafeFiles.tempPathFor(file)));
    }
}
