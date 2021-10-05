package net.creeperhost.creeperlauncher.minecraft.modloader.forge;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.minecraft.McUtils;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.ForgeUtils;
import net.creeperhost.creeperlauncher.util.LoaderTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForgeUniversalModLoader extends ForgeModLoader
{
    private static final Logger LOGGER = LogManager.getLogger();

	public ForgeUniversalModLoader(List<LoaderTarget> loaderTargets)
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
		instance.modLoader = newname;
        LOGGER.info("Minecraft version: {} Forge version: {}", getMinecraftVersion(), getForgeVersion());
        Path file = Constants.VERSIONS_FOLDER_LOC.resolve(newname);
        FileUtils.createDirectories(file);

		try
		{
			URI url = ForgeUtils.findForgeDownloadURL(getMinecraftVersion(), getForgeVersion());
			Path forgeFile = file.resolve(newname + ".jar");
			DownloadableFile forge = new DownloadableFile(getForgeVersion(), forgeFile, url.toString(), Collections.emptyList(), 0, 0, newname, "modloader", String.valueOf(System.currentTimeMillis() / 1000L));
			DownloadTask task = new DownloadTask(forge, forgeFile);
			task.execute().join();

            LOGGER.info("Completed download of {}", newname);

			if (Files.exists(forgeFile))
			{
				boolean extracted = ForgeUtils.extractJson(forgeFile, newname + ".json");
				Path forgeJson = file.resolve(newname + ".json");
				if(!extracted)
				{
                    LOGGER.error("Failed to extract version json, attempting to download it from repo");
					String downloadName = "forge-" + getMinecraftVersion() + ".json";
					DownloadableFile fjson = new DownloadableFile(forgeJson.getFileName().toString(), forgeJson, "https://apps.modpacks.ch/versions/minecraftjsons/" + downloadName, Collections.emptyList(), 0, 0, downloadName, "modloader", String.valueOf(System.currentTimeMillis() / 1000L));
					DownloadTask ftask = new DownloadTask(fjson, forgeJson);
					ftask.execute().join();
				}
				if (Files.exists(forgeJson))
				{
					ForgeUtils.updateForgeJson(forgeJson, newname, getMinecraftVersion());
					//Move the forge jar to its home in libs
                    Path libForgeDir = Constants.LIBRARY_LOCATION.resolve("net/minecraftforge/forge/" + getMinecraftVersion() + "-" + getForgeVersion());
					FileUtils.createDirectories(libForgeDir);
					Path forgeLib = libForgeDir.resolve("forge-" + getMinecraftVersion() + "-" + getForgeVersion() + ".jar");
					if (!Files.exists(forgeLib)) Files.copy(forgeFile, forgeLib, StandardCopyOption.REPLACE_EXISTING);

					returnFile = forgeJson;
				} else
				{
                    LOGGER.error("Failed to get the 'version.json' for '{}'", newname);
				}
			}
		} catch (Throwable e)
		{
		    LOGGER.error(e);
		}
		return returnFile;
	}

	@Override
	public boolean isApplicable()
	{
		int minorMcVersion = McUtils.parseMinorVersion(getTargetVersion("minecraft").orElse("0.0.0"));

		if (minorMcVersion == 12) {
			String[] versions = getForgeVersion().split("\\.");
			try {
				int forgeVer = Integer.parseInt(versions[versions.length - 1]);
				return super.isApplicable() && forgeVer < 2851;
			} catch (NumberFormatException ignored) {
				// ¯\_(ツ)_/¯
			}
		}
		//1.6 -> 1.12.2
		return super.isApplicable() && minorMcVersion >= 6 && minorMcVersion <= 12;
	}
}
