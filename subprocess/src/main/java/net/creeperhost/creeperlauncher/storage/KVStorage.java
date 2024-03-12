package net.creeperhost.creeperlauncher.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
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
    
    private final Map<String, String> data;

    private static KVStorage INSTANCE;

    public static KVStorage getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KVStorage(load());
        }

        return INSTANCE;
    }

    public KVStorage(Map<String, String> data) {
        this.data = data;
    }

    private static Map<String, String> load() {
        if (Files.exists(Constants.KV_STORE_FILE)) {
            try {
                return JsonUtils.parse(GSON, Constants.KV_STORE_FILE, MAP_TYPE);
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.error("Failed to read generic storage", e);
            }
        }
        return new HashMap<>();
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
        try {
            JsonUtils.write(GSON, IOUtils.makeParents(Constants.KV_STORE_FILE), data, MAP_TYPE);
        } catch (IOException e) {
            LOGGER.fatal("Failed to write data to the storage file", e);
            return false;
        }

        return true;
    }
}
