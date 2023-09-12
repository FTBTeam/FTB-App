package net.creeperhost.creeperlauncher.minecraft;

import com.google.gson.*;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class McUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    //Could be any modloader
    public static List<LoaderTarget> getTargets(Path instanceDir) {
        List<LoaderTarget> targetList = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(instanceDir.resolve("version.json"))) {
            JsonObject obj = GsonUtils.GSON.fromJson(reader, JsonObject.class);
            JsonArray targets = obj.getAsJsonArray("targets");
            if (targets != null) {
                for (JsonElement serverEl : targets) {
                    JsonObject server = (JsonObject) serverEl;
                    String targetVersion = server.get("version").getAsString();
                    long targetId = server.get("id").getAsLong();
                    String targetName = server.get("name").getAsString();
                    String targetType = server.get("type").getAsString();

                    targetList.add(new LoaderTarget(targetName, targetVersion, targetId, targetType));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load version json.", e);
        }
        return targetList;
    }
}


