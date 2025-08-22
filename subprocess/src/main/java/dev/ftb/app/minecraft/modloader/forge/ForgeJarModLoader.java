package dev.ftb.app.minecraft.modloader.forge;

import dev.ftb.app.AppMain;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Deprecated
public class ForgeJarModLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    @Deprecated // This can be done very differently!
	public static void prePlay(Instance instance) {
		try {
			LOGGER.info("Pre-Play started");
			String newname = instance.getModLoader();

			Path instanceDir = instance.getDir();
			Path instMods = instanceDir.resolve("instmods");
            Path jarMods = instanceDir.resolve("jarmods");

            LOGGER.info("intmods location: {}", instMods.toAbsolutePath());
            Path mcFile = instMods.resolve("minecraft.jar");
            LOGGER.info("mc location: {}", mcFile.toAbsolutePath());

			//Merge Jars, This will be a prePlayTask in release code
			Path libVersionDir = AppMain.paths().mcLibrariesDir().resolve("net/minecraftforge/forge/" + instance.getMcVersion() + "-" + instance.getModLoader());
            LOGGER.info("LibVersionDir: {}", libVersionDir);
			FileUtils.createDirectories(libVersionDir);
            Path forgeVersion = libVersionDir.resolve("forge-" +instance.getMcVersion() + "-" + instance.getModLoader() + ".jar");
            LOGGER.info("forgeVersion: {}", forgeVersion);
            Path versionsJar = AppMain.paths().mcVersionsDir().resolve(newname).resolve(newname + ".jar");
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
}
