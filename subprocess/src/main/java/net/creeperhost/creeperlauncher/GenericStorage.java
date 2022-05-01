package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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

    private static final Path STORAGE_FILE = Constants.getDataDir().resolve("storage/storage.json");
    private Map<String, String> data = new HashMap<>();

    private static GenericStorage INSTANCE;
    
    public static GenericStorage getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GenericStorage();
        }
        
        return INSTANCE;
    }

    public GenericStorage() {
        this.load();
    }

    private void load() {
        if (!Files.exists(STORAGE_FILE)) {
            return;
        }

        try {
            data = new Gson().fromJson(Files.readString(STORAGE_FILE), new TypeToken<Map<String, String>>() {}.getType());
        } catch (IOException e) {
            LOGGER.error("Failed to read generic storage", e);
        }
    }

    public String getAllAsJson() {
        return new Gson().toJson(data);
    }

    public String getValue(String key) {
        return data.get(key);
    }

    public boolean put(String key, String value) {
        data.put(key, value);
        return save();
    }

    private boolean save() {
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
            Files.writeString(STORAGE_FILE, gson.toJson(data));
        } catch (IOException e) {
            LOGGER.fatal("Failed to write data to the storage file", e);
            return false;
        }

        return true;
    }
}
