package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Super basic file based key -> value store. We're going to leave it up to the frontend to parse the data.
 * This is a dumb storage where all it needs to know is the key and the generic value.
 *
 * This is very similar to how localStorage works in electron, but I want it to save to file instead of the app.
 */
public class GenericStorage {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Path STORAGE_FILE = Constants.WORKING_DIR.resolve("storage/storage.json");
    private static final Map<String, String> DATA = new HashMap<>();

    public static String getAllAsJson() {
        return new Gson().toJson(DATA);
    }

    public static String getValue(String key) {
        return DATA.get(key);
    }

    public static boolean put(String key, String value) {
        DATA.put(key, value);
        return save();
    }

    private static boolean save() {
        // Create the folder if it's missing
        Path dir = STORAGE_FILE.getParent();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                LOGGER.fatal("Failed to create storage directory ({})", dir, e);
                return false; // Stop, we can't save
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.writeString(STORAGE_FILE, gson.toJson(DATA));
        } catch (IOException e) {
            LOGGER.fatal("Failed to write data to the storage file", e);
            return false;
        }

        return true;
    }
}
