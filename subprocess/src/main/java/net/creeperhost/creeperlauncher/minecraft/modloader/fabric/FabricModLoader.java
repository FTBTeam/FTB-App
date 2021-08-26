package net.creeperhost.creeperlauncher.minecraft.modloader.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoader;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class FabricModLoader extends ModLoader
{
    private static final Logger LOGGER = LogManager.getLogger();

	private static final String FABRIC_MET_URL = "https://meta.fabricmc.net/v2/";
	private static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";

	public FabricModLoader(List<LoaderTarget> loaderTargets)
	{
		super(loaderTargets);
	}

	@Override
	public String getName()
	{
		return "fabric";
	}

	@Override
	public Path install(LocalInstance instance)
	{
        LOGGER.info("Minecraft version: {} Fabric version: {}", getMinecraftVersion(), getFabricVersion());
		var profileName = String.format("fabric-loader-%s-%s", getFabricVersion(), getMinecraftVersion());

		instance.modLoader = profileName;

		JsonObject loaderMeta;
		try
		{
			loaderMeta = getInstallMeta();
		} catch (IOException e) {
            LOGGER.error("Failed to get fabric install meta", e);
			return null;
		}

		var launcherMeta = loaderMeta.get("launcherMeta").getAsJsonObject();
		var libraries = (JsonArray) launcherMeta.get("libraries").getAsJsonObject().get("common");

		//Add intermediary and fabric-loader as those are not included in the metadata
		libraries.add(getLibrary("net.fabricmc:intermediary:" + getMinecraftVersion(), FABRIC_MAVEN_URL));
		libraries.add(getLibrary("net.fabricmc:fabric-loader:" + getFabricVersion(), FABRIC_MAVEN_URL));

		var versionsDir = Constants.VERSIONS_FOLDER_LOC;
		var profileDir = versionsDir.resolve(profileName);
		var profileJson = profileDir.resolve(profileName + ".json");

        FileUtils.createDirectories(profileDir);

		Path dummyJar = profileDir.resolve(profileName + ".jar");
		try
		{
			if (!Files.exists(dummyJar))
			{
				Files.createFile(dummyJar);
			}
		} catch (IOException e) {
            LOGGER.error("Failed to create fabric jar, is the game running?", e);
			return null;
		}

		var currentTime = MiscUtils.ISO_8601.format(new Date());

		JsonObject profile = new JsonObject();
		profile.addProperty("id", profileName);
		profile.addProperty("inheritsFrom", getMinecraftVersion());
		profile.addProperty("releaseTime", currentTime);
		profile.addProperty("time", currentTime);
		profile.addProperty("type", "release");

		profile.addProperty("mainClass", launcherMeta.get("mainClass").getAsJsonObject().get("client").getAsString());

		var arguments = new JsonObject();
		arguments.add("game", new JsonArray());
		profile.add("arguments", arguments);

		profile.add("libraries", libraries);

		try
		{
			Files.write(profileJson, GsonUtils.GSON.toJson(profile).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
            LOGGER.error("Failed to create fabric profile json, is the game running?", e);
			return null;
		}

		return null;
	}

	private JsonObject getInstallMeta() throws IOException
	{
		var metaUrl = String.format("%sversions/loader/%s/%s", FABRIC_MET_URL, getMinecraftVersion(), getFabricVersion());
		var meta = DownloadUtils.urlToString(new URL(metaUrl));
		return GsonUtils.GSON.fromJson(meta, JsonObject.class);
	}

	private JsonObject getLibrary(String mavenPath, String url)
	{
		var jsonObject = new JsonObject();
		jsonObject.addProperty("name", mavenPath);
		jsonObject.addProperty("url", url);
		return jsonObject;
	}

	public String getFabricVersion()
	{
		return getTargetVersion("fabric").orElseThrow();
	}
}
