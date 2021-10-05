package net.creeperhost.creeperlauncher.minecraft.modloader;

import net.creeperhost.creeperlauncher.minecraft.modloader.fabric.FabricModLoader;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeInstallerModLoader;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeJarModLoader;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeUniversalModLoader;
import net.creeperhost.creeperlauncher.util.LoaderTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModLoaderManager {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final List<ModLoaderFactory<?>> MOD_LOADER_FACTORIES = new ArrayList<>();

	public static final ModLoaderFactory<ForgeInstallerModLoader> FORGE_INSTALLER = register(ForgeInstallerModLoader::new);
	public static final ModLoaderFactory<ForgeUniversalModLoader> FORGE_UNIVERSAL = register(ForgeUniversalModLoader::new);
	public static final ModLoaderFactory<ForgeJarModLoader> FORGE_JAR = register(ForgeJarModLoader::new);
	public static final ModLoaderFactory<FabricModLoader> FABRIC = register(FabricModLoader::new);

	private static <T extends ModLoader> ModLoaderFactory<T> register(ModLoaderFactory<T> modLoaderFactory)
	{
		MOD_LOADER_FACTORIES.add(modLoaderFactory);
		return modLoaderFactory;
	}

	public static List<ModLoader> getModLoaders(List<LoaderTarget> loaderTargets)
	{
		//TODO This is deduplicate the list, This should not be needed but fixes an outstanding issue
		List<ModLoader> output = new ArrayList<>();
		for(ModLoaderFactory<?> factory : MOD_LOADER_FACTORIES)
		{
			ModLoader target = factory.create(loaderTargets);
			if(!output.contains(target) && target.isApplicable())
			{
				boolean exists = false;
				for(ModLoader modLoader : output)
				{
					if(modLoader.getName().equals(target.getName()))
					{
						exists = true;
					}
				}
				if(!exists)
				{
					output.add(target);
				}
			}
		}
		output.forEach(modLoader -> LOGGER.debug(modLoader.getName()));
		return output;
//		return MOD_LOADER_FACTORIES.stream()
//				.map(modLoaderFactory -> modLoaderFactory.create(loaderTargets))
//				.filter(ModLoader::isApplicable)
//				.collect(Collectors.toList());
	}

	@FunctionalInterface
	public interface ModLoaderFactory<T extends ModLoader>
	{
		T create(List<LoaderTarget> loaderTargets);
	}
}
