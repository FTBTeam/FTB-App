package net.creeperhost.creeperlauncher.minecraft.modloader.forge;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.minecraft.McUtils;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.ForgeUtils;
import net.creeperhost.creeperlauncher.util.LoaderTarget;
import net.creeperhost.creeperlauncher.util.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForgeJarModLoader extends ForgeModLoader
{
    private static final Logger LOGGER = LogManager.getLogger();

	public ForgeJarModLoader(List<LoaderTarget> loaderTargets)
	{
		super(loaderTargets);
	}

	@Override
	public String getName()
	{
		return "forge";
	}

	@Override
	public Path install(LocalInstance instance)
	{
		Path returnFile = null;
		String newname = getMinecraftVersion() + "-forge" + getMinecraftVersion() + "-" + getForgeVersion();
		instance.mcVersion = getMinecraftVersion();
		instance.modLoader = newname;
		instance.hasInstMods = true;

		LOGGER.info("Minecraft version: {} Forge version: {} NewName: {}", getMinecraftVersion(), getForgeVersion(), newname);

		Path file = Constants.VERSIONS_FOLDER_LOC.resolve(newname);
		FileUtils.createDirectories(file);

		//Add the jvm args to fix loading older forge versions
		instance.jvmArgs = instance.jvmArgs + " -Dfml.ignorePatchDiscrepancies=true -Dfml.ignoreInvalidMinecraftCertificates=true -Dminecraft.applet.TargetDirectory=\"" + instance.getDir().toAbsolutePath().toString().trim() + "\"";
		try
		{
			URI url = null;
			try
			{
				url = ForgeUtils.findForgeDownloadURL(getMinecraftVersion(), getForgeVersion());
			} catch (URISyntaxException | MalformedURLException e)
			{
				e.printStackTrace();
			}
            Path instMods = instance.getDir().resolve("instmods");
			Files.createDirectories(instMods);

            Path forgeFile = instMods.resolve(newname + ".jar");
			if(Files.notExists(forgeFile))
			{
				LOGGER.info(forgeFile.toAbsolutePath() + " Does not exist, Downloading from " + url.toString());

				DownloadableFile forge = new DownloadableFile(newname, forgeFile, url.toString(), Collections.emptyList(), 0, 0, newname, "modloader", String.valueOf(System.currentTimeMillis() / 1000L));
				DownloadTask task = new DownloadTask(forge, forgeFile);
				task.execute();
			}

            Path mcFile = instMods.resolve("minecraft.jar");
			if(Files.notExists(mcFile))
			{
				LOGGER.info(mcFile.toAbsolutePath() + " Does not exist, Downloading from Mojang servers");
				DownloadableFile mc = McUtils.getMinecraftDownload(getMinecraftVersion(), instMods);
				DownloadTask mcTask = new DownloadTask(mc, mcFile);
				mcTask.execute();
			}

			Path forgeJson = file.resolve(newname + ".json");
			if(Files.notExists(forgeJson))
			{
				String downloadName = "forge-" + getMinecraftVersion() + ".json";
				String jsonurl = "https://apps.modpacks.ch/versions/minecraftjsons/" + downloadName;

				LOGGER.error("Failed to extract version json, attempting to download it from repo " + jsonurl);

				if(WebUtils.checkExist(new URL(jsonurl)))
				{
					DownloadableFile fjson = new DownloadableFile(forgeJson.getFileName().toString(), forgeJson, jsonurl, Collections.emptyList(), 0, 0, downloadName, "modloader", String.valueOf(System.currentTimeMillis() / 1000L));
					DownloadTask ftask = new DownloadTask(fjson, forgeJson);
					ftask.execute().join();
				}
				else
				{
                    LOGGER.error("Failed to download {} from repo", downloadName);
				}
			}

			if(Files.exists(forgeJson))
			{
				boolean val = ForgeUtils.updateForgeJson(forgeJson, newname, getMinecraftVersion(), true);
				LOGGER.info(val ? "ForgeJson has been updated at " + forgeJson.toAbsolutePath() : "Failed to update ForgeJson " + forgeJson.toAbsolutePath());
				returnFile = forgeJson;
			}

			LOGGER.info("Forge Install finished");
			return returnFile;
		} catch (Exception exception)
		{
			exception.printStackTrace();
		}
		return returnFile;
	}

	public static void prePlay(LocalInstance instance)
	{
		try
		{
			LOGGER.info("Pre-Play started");
			String newname = instance.getModLoader();

			Path instanceDir = instance.getDir();
			Path instMods = instanceDir.resolve("instmods");
            Path jarMods = instanceDir.resolve("jarmods");

            LOGGER.info("intmods location: {}", instMods.toAbsolutePath());
            Path mcFile = instMods.resolve("minecraft.jar");
            LOGGER.info("mc location: {}", mcFile.toAbsolutePath());

			//Merge Jars, This will be a prePlayTask in release code
			Path libVersionDir = Constants.LIBRARY_LOCATION.resolve("net/minecraftforge/forge/" + instance.getMcVersion() + "-" + instance.getModLoader());
            LOGGER.info("LibVersionDir: {}", libVersionDir);
			FileUtils.createDirectories(libVersionDir);
            Path forgeVersion = libVersionDir.resolve("forge-" +instance.getMcVersion() + "-" + instance.getModLoader() + ".jar");
            LOGGER.info("forgeVersion: {}", forgeVersion);
            Path versionsJar = Constants.VERSIONS_FOLDER_LOC.resolve(newname).resolve(newname + ".jar");
			FileUtils.createDirectories(versionsJar.getParent());

			//Remove the forge jar that is loaded so we can build a new one, This will be required for us to load newly added core mods
			Files.deleteIfExists(forgeVersion);

			if (Files.exists(mcFile))
			{
                LOGGER.info("mc file exists, attempting to merge jars");
				try
				{
					Path merged = instMods.resolve("merged.jar");

					//Remove the prebuilt jar so we can make a fresh one
					Files.deleteIfExists(merged);

					Files.copy(mcFile, merged);

					FileUtils.removeMeta(merged);

					List<Path> instFiles = FileUtils.listDir(instMods);
                    List<Path> jarFiles = FileUtils.listDir(jarMods);

					if (instFiles != null) {
                        LOGGER.info("instmod folder has mods to merge, attempting to merge jars");
						//Merge every file in the instmods folder that is not the mc jar or the merge target
						for (Path instFile : instFiles) {
							if (instFile != null && !instFile.equals(merged)) {
								if (!instFile.getFileName().toString().contains("minecraft") && !instFile.getFileName().toString().contains("merged")) {
                                    LOGGER.info("Merging {} into the merged.jar", instFile.getFileName());
									if (!FileUtils.mergeJars(instFile, merged)) {
                                        LOGGER.error("Filed to merge {} into merged.jar", instFile.getFileName());
									}
								}
							}
						}

						if (jarFiles != null) {
                            LOGGER.info("jarmods folder has mods to merge, attempting to merge jars");
							//Merge every file in the instmods folder that is not the mc jar or the merge target
							for (Path instFile : jarFiles) {
								if (!instFile.equals(merged)) {
									if (!instFile.getFileName().toString().contains("minecraft") && !instFile.getFileName().toString().contains("merged")) {
                                        LOGGER.info("Merging {} into the merged.jar", instFile.getFileName());
										if (!FileUtils.mergeJars(instFile, merged)) {
                                            LOGGER.error("Filed to merge {} into merged.jar", instFile.getFileName());
										}
									}
								}
							}
						}
					}
					//Move the merged jar to it location in the libs folder to load
					if (Files.exists(merged)) {
						Files.copy(merged, versionsJar, StandardCopyOption.REPLACE_EXISTING);
						Files.copy(merged, forgeVersion, StandardCopyOption.REPLACE_EXISTING);
					}
                    LOGGER.info("All files successfully merged");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean isApplicable()
	{
		int minorMcVersion = McUtils.parseMinorVersion(getTargetVersion("minecraft").orElse("0.0.0"));
		//1.2.5 -> 1.5.2
		return super.isApplicable() && minorMcVersion >= 2 && minorMcVersion <= 5;
	}
}
