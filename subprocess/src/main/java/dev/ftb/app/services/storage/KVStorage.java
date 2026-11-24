package dev.ftb.app.services.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.AppPaths;
import dev.ftb.app.util.JsonStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Super basic file based key -> value store. We're going to leave it up to the frontend to parse the data.
 * This is a dumb storage where all it needs to know is the key and the generic value.
 * <p>
 * This is very similar to how localStorage works in electron, but I want it to save to file instead of the app.
 */
public class KVStorage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
    
    private final JsonStore<Map<String, String>> store;
    
    public KVStorage(AppPaths paths) {
        this.store = new JsonStore<>(
            paths.keyValueFile(),
            GSON,
            MAP_TYPE,
            HashMap::new
        );
    }
    
    public void init() {
        try {
            this.store.load();
        } catch (IOException e) {
            LOGGER.info("Failed to load key value store, creating a new one", e);
        }
    }

    public String get(String key) {
        return this.store.get().get(key);
    }

    public String getAllAsJson() {
        return new Gson().toJson(this.store.get());
    }

    public boolean put(String key, String value) {
        try {
            this.store.update(existing -> existing.put(key, value));
        } catch (IOException e) {
            LOGGER.error("Failed to put value into key value store", e);
            return false;
        }
        
        return true;
    }
}
