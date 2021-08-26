package net.creeperhost.creeperlauncher.minecraft.modloader.forge;

import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoader;
import net.creeperhost.creeperlauncher.util.ForgeUtils;
import net.creeperhost.creeperlauncher.util.LoaderTarget;

import java.util.List;

public abstract class ForgeModLoader extends ModLoader
{
	public ForgeModLoader(List<LoaderTarget> loaderTargets)
	{
		super(loaderTargets);
	}

	public String getForgeVersion()
	{
		String version = getTargetVersion("forge").orElseThrow();
		if (version.contains("recommended"))
		{
			version = ForgeUtils.getRecommended(getMinecraftVersion());
			return version;
		}
		else if (version.contains("latest"))
		{
			version = ForgeUtils.getLatest(getMinecraftVersion());
			return version;
		}
		return version;
	}
}
