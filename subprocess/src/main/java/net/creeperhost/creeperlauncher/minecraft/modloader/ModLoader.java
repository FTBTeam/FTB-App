package net.creeperhost.creeperlauncher.minecraft.modloader;

import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.LoaderTarget;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public abstract class ModLoader {

	private final List<LoaderTarget> loaderTargets;

	public ModLoader(List<LoaderTarget> loaderTargets)
	{
		this.loaderTargets = loaderTargets;
	}

	public abstract String getName();

	public abstract Path install(Instance instance);

	public boolean isApplicable()
	{
		var optionalTarget = getTargetFromName(getName());
		if (optionalTarget.isPresent()) {
			var target = optionalTarget.get();
			return target.getType().equals("modloader") && target.getName().equals(getName());
		}
		return getTargetFromName(getName()).isPresent();
	}

	public String getMinecraftVersion()
	{
		return getTargetVersion("minecraft").orElseThrow();
	}

	public Optional<String> getTargetVersion(String name)
	{
		return getTargetFromName(name)
				.map(LoaderTarget::getVersion);
	}

	public Optional<LoaderTarget> getTargetFromName(String name)
	{
		for (LoaderTarget target : loaderTargets) {
			if (target.getName().equals(name)) {
				return Optional.of(target);
			}
		}
		return Optional.empty();
	}

}
