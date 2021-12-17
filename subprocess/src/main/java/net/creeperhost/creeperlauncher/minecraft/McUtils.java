package net.creeperhost.creeperlauncher.minecraft;

import com.google.common.hash.HashCode;
import com.google.gson.*;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class McUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson = new Gson();

    public static String getMinecraftJsonForVersion(String version) {
        String url = Constants.MC_VERSION_MANIFEST;
        String resp = WebUtils.getAPIResponse(url);

        JsonElement jElement = new JsonParser().parse(resp);
        if (jElement.isJsonObject()) {
            JsonArray jsonArray = jElement.getAsJsonObject().getAsJsonArray("versions");

            if (jsonArray != null) {
                for (JsonElement serverEl : jsonArray) {
                    JsonObject server = (JsonObject) serverEl;
                    String name = server.get("id").getAsString();
                    String url1 = server.get("url").getAsString();

                    if (name.equalsIgnoreCase(version)) {
                        return url1;
                    }
                }
            }
        }
        return null;
    }

    public static DownloadableFile getMinecraftDownload(String version, Path downloadLoc) {
        String jsonURL = getMinecraftJsonForVersion(version);
        String resp = WebUtils.getWebResponse(jsonURL);

        JsonElement jElement = new JsonParser().parse(resp);
        if (jElement.isJsonObject()) {
            JsonObject jsonObject = jElement.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("client");
            HashCode sha1 = HashCode.fromString(jsonObject.get("sha1").getAsString());
            long size = jsonObject.get("size").getAsLong();
            String URL = jsonObject.get("url").getAsString();

            List<HashCode> sha1List = new ArrayList<>();
            sha1List.add(sha1);

            return new DownloadableFile(downloadLoc, URL, sha1List, size, 0, version, "");
        }
        return null;
    }

    public static boolean removeProfile(Path target, String profileID)
    {
        verifyJson(target);
        if(target == null || !target.toFile().exists())
        {
            LOGGER.error("launcher_profiles.json does not exist at " + target);
        }
        LOGGER.info("Attempting to remove {}", profileID);
        try {
            JsonObject json = null;
            try (BufferedReader reader = Files.newBufferedReader(target)) {
                json = gson.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                LOGGER.error("Failed to read {}", target, e);
                try {
                    URL url = new URL("https://apps.modpacks.ch/FTB2/launcher_profiles.json");
                    URLConnection urlConnection = url.openConnection();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String buffer;
                    while((buffer = bufferedReader.readLine()) != null)
                    {
                        stringBuilder.append(buffer);
                    }
                    json = new JsonParser().parse(stringBuilder.toString()).getAsJsonObject();
                } catch (Throwable t)
                {
                    e.printStackTrace();
                    return false;
                }
            }

            JsonObject _profiles = json.getAsJsonObject("profiles");
            if (_profiles == null) {
                _profiles = new JsonObject();
                json.add("profiles", _profiles);
            }

            JsonObject _profile = _profiles.getAsJsonObject(profileID);
            if (_profile != null) {
                _profiles.remove(profileID);
                String jstring = GsonUtils.GSON.toJson(json);
                Files.write(target, jstring.getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Removed profile {}", profileID);
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("There was a problem writing the launch profile, is it write protected?", e);
            return false;
        }
        return false;
    }

    public static void verifyJson(Path target)
    {
        if(Files.exists(target))
        {
            try (BufferedReader reader = Files.newBufferedReader(target))
            {
                JsonObject json = null;
                try
                {
                    gson.fromJson(reader, JsonObject.class);
                }
                catch (JsonSyntaxException e)
                {
                    LOGGER.error("Error reading launcher_profiles.json", e);
                    //Json is malformed
                    Files.delete(target);
                    LOGGER.info("launcher_profiles.json removed, Attempting to download new launcher_profiles.json");
                    DownloadUtils.downloadFile(target, "https://apps.modpacks.ch/FTB2/launcher_profiles.json");
                }
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to verify json.", e);
            }
        }
    }

    public static int parseMinorVersion(String minecraftVersion) {
        String[] split = minecraftVersion.split("\\.");
        if (split.length >= 2) {
            return Integer.parseInt(split[1]);
        }
        return -1;
    }

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


