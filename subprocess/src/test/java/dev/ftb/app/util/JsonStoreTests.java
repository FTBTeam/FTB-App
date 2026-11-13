package dev.ftb.app.util;

import com.google.gson.Gson;
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

import static org.junit.jupiter.api.Assertions.*;

public class JsonStoreTests {
    private static final Gson GSON = new Gson();

    @TempDir
    Path dir;

    static class Data {
        int count;
        String name = "default";
        List<Integer> items = new ArrayList<>();
    }

    private JsonStore<Data> store(Path file) {
        return new JsonStore<>(file, GSON, Data.class, Data::new);
    }

    @Test
    public void testMissingFileUsesDefaults() throws IOException {
        var store = store(dir.resolve("data.json"));
        assertEquals(JsonStore.LoadResult.MISSING_USING_DEFAULTS, store.load());
        assertEquals("default", store.get().name);
    }

    @Test
    public void testRoundTrip() throws IOException {
        Path file = dir.resolve("nested/data.json");
        store(file).update(d -> {
            d.count = 5;
            d.name = "saved";
        });

        var reloaded = store(file);
        assertEquals(JsonStore.LoadResult.LOADED, reloaded.load());
        assertEquals(5, reloaded.get().count);
        assertEquals("saved", reloaded.get().name);
        assertFalse(Files.exists(dir.resolve("nested/data.json.tmp")));
    }

    @Test
    public void testSecondSaveKeepsBackupOfPrevious() throws IOException {
        Path file = dir.resolve("data.json");
        var store = store(file);
        store.update(d -> d.count = 1);
        store.update(d -> d.count = 2);

        var backup = GSON.fromJson(Files.readString(dir.resolve("data.json.bak")), Data.class);
        assertEquals(1, backup.count);
    }

    @Test
    public void testEmptyFileIsQuarantined() throws IOException {
        Path file = dir.resolve("data.json");
        Files.writeString(file, "");

        var store = store(file);
        assertEquals(JsonStore.LoadResult.CORRUPT_RESET_TO_DEFAULTS, store.load());
        assertEquals("default", store.get().name);
        assertFalse(Files.exists(file));
        assertTrue(hasCorruptFile("data.json.corrupt-"));
    }

    @Test
    public void testCorruptFileRecoversFromBackup() throws IOException {
        Path file = dir.resolve("data.json");
        Files.writeString(file, "{\"count\": 3, \"na");
        Files.writeString(dir.resolve("data.json.bak"), "{\"count\": 2, \"name\": \"backup\"}");

        var store = store(file);
        assertEquals(JsonStore.LoadResult.RECOVERED_FROM_BACKUP, store.load());
        assertEquals("backup", store.get().name);
        assertTrue(hasCorruptFile("data.json.corrupt-"));

        // The real file is rewritten from the backup
        assertEquals(JsonStore.LoadResult.LOADED, store(file).load());
    }

    @Test
    public void testLeftoverTempFileIsIgnored() throws IOException {
        Path file = dir.resolve("data.json");
        Files.writeString(file, "{\"count\": 1}");
        Files.writeString(dir.resolve("data.json.tmp"), "{\"count\": 99, \"na");

        var store = store(file);
        assertEquals(JsonStore.LoadResult.LOADED, store.load());
        assertEquals(1, store.get().count);
        assertFalse(Files.exists(dir.resolve("data.json.tmp")));
    }

    @Test
    public void testMigratorIsApplied() throws IOException {
        Path file = dir.resolve("data.json");
        Files.writeString(file, "{\"oldName\": \"migrated\"}");

        var store = new JsonStore<>(file, GSON, Data.class, Data::new, json -> {
            var obj = json.getAsJsonObject();
            obj.add("name", obj.remove("oldName"));
            return obj;
        });
        assertEquals(JsonStore.LoadResult.LOADED, store.load());
        assertEquals("migrated", store.get().name);
    }

    @Test
    public void testConcurrentUpdatesAreNotLost() throws Exception {
        Path file = dir.resolve("data.json");
        var store = store(file);

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    futures.add(executor.submit(() -> {
                        store.update(d -> d.count++);
                        return null;
                    }));
                }
                for (var future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdown();
            }
        }

        var reloaded = store(file);
        reloaded.load();
        assertEquals(100, reloaded.get().count);
    }

    @Test
    public void testSnapshotsAreNeverChangedByLaterWrites() throws IOException {
        var store = store(dir.resolve("data.json"));
        store.update(d -> d.name = "first");
        var snapshot = store.get();

        store.update(d -> d.name = "second");

        assertEquals("first", snapshot.name);
        assertEquals("second", store.get().name);
        assertNotSame(snapshot, store.get());
    }

    @Test
    public void testSetKeepsItsOwnCopy() throws IOException {
        var store = store(dir.resolve("data.json"));
        var value = new Data();
        value.name = "set";
        store.set(value);

        value.name = "changed after set";
        assertEquals("set", store.get().name);
    }

    @Test
    public void testUpdateIfThatFailsChangesNothing() throws IOException {
        var store = store(dir.resolve("data.json"));
        store.update(d -> d.count = 1);
        var before = store.get();

        assertFalse(store.updateIf(d -> d.count == 99, d -> d.count = 2));
        assertSame(before, store.get());
    }

    @Test
    public void testFailedSaveLeavesTheDataUnchanged() throws IOException {
        Path file = dir.resolve("data.json");
        var store = store(file);
        store.update(d -> d.count = 1);

        // Make the save fail by putting a non-empty directory where the file goes
        Files.delete(file);
        Files.createDirectories(file.resolve("blocker"));

        assertThrows(IOException.class, () -> store.update(d -> d.count = 2));
        assertEquals(1, store.get().count);
    }

    @Test
    public void testReadersCanIterateWhileOthersWrite() throws Exception {
        var store = store(dir.resolve("data.json"));

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < 50; i++) {
                    int value = i;
                    futures.add(executor.submit(() -> {
                        store.update(d -> d.items.add(value));
                        return null;
                    }));
                    // With a shared live list this iteration would throw ConcurrentModificationException
                    futures.add(executor.submit(() -> {
                        for (int j = 0; j < 20; j++) {
                            int sum = 0;
                            for (int item : store.get().items) {
                                sum += item;
                            }
                            assertTrue(sum >= 0);
                        }
                        return null;
                    }));
                }
                for (var future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdown();
            }
        }

        assertEquals(50, store.get().items.size());
    }

    private boolean hasCorruptFile(String prefix) throws IOException {
        try (var files = Files.list(dir)) {
            return files.anyMatch(p -> p.getFileName().toString().startsWith(prefix));
        }
    }
}
