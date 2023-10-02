package net.creeperhost.creeperlauncher.install;

import com.google.gson.JsonParseException;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.data.InstanceModifications;
import net.creeperhost.creeperlauncher.data.InstanceModifications.ModOverride;
import net.creeperhost.creeperlauncher.data.InstanceModifications.ModOverrideState;
import net.creeperhost.creeperlauncher.data.mod.CurseMetadata;
import net.creeperhost.creeperlauncher.data.mod.ModInfo;
import net.creeperhost.creeperlauncher.data.mod.ModManifest;
import net.creeperhost.creeperlauncher.install.tasks.*;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 12/9/23.
 */
public class ModInstaller implements ModCollector {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Instance instance;

    private final String mcVersion;
    private final String modLoader;
    private final OperationProgressTracker tracker;

    private final List<Pair<ModManifest, ModManifest.Dependency>> unavailable = new ArrayList<>();
    private final List<Pair<ModManifest, ModManifest>> unsatisfiable = new ArrayList<>();
    private final List<Pair<ModManifest, ModManifest.Version>> toInstall = new ArrayList<>();

    public ModInstaller(Instance instance, String mcVersion, String modLoader) {
        this.instance = instance;
        this.mcVersion = mcVersion;
        this.modLoader = modLoader;
        tracker = new OperationProgressTracker(
                "mod_install",
                Map.of(
                        "instance", instance.getUuid().toString()
                )
        );
    }

    public void resolve(long modId, long versionId) throws ModInstallerException {
        LOGGER.info("Resolving install of mod {} version {}", modId, versionId);

        tracker.nextStage(InstallStage.RESOLVE);
        try {
            ModManifest manifest;
            try {
                manifest = ModManifest.queryManifest(modId);
            } catch (IOException | JsonParseException ex) {
                throw new ModInstallerException("Mod does not exist.", ex);
            }

            ModManifest.Version version = manifest.findVersion(versionId);
            if (version == null) {
                throw new ModInstallerException("Mod version does not exist.");
            }

            manifest.visitVersion(this, version);
        } catch (Throwable ex) {
            tracker.finished();
            throw ex;
        }
        List<ModInfo> existingMods = instance.getMods();
        LOGGER.info("Filtering found dependencies from existing mod list.");
        toInstall.removeIf(pair -> {
            ModManifest manifest = pair.getKey();
            ModManifest.Version version = pair.getValue();
            for (ModInfo existingMod : existingMods) {
                CurseMetadata curse = existingMod.curse();
                if (curse == null) continue;

                // TODO, What if the version is different, should we update instead?
                if (manifest.getId() == curse.curseProject()) {
                    LOGGER.info("Removing already satisfied dependency: {}({}) ({}){}", manifest.getName(), manifest.getId(), version.getId(), version.getName());
                    return true;
                }
            }
            return false;
        });

        LOGGER.info("The following mods will be installed:");
        for (Pair<ModManifest, ModManifest.Version> pair : toInstall) {
            ModManifest manifest = pair.getKey();
            ModManifest.Version version = pair.getValue();
            LOGGER.info(" {}({}) ({}){}", manifest.getName(), manifest.getId(), version.getId(), version.getName());
        }

        LOGGER.info("The following mods could not be found:");
        for (Pair<ModManifest, ModManifest.Dependency> pair : unavailable) {
            ModManifest requiredBy = pair.getKey();
            ModManifest.Dependency dep = pair.getRight();
            LOGGER.info(" {}({}) Requires Project {}", requiredBy.getName(), requiredBy.getId(), dep.getId());
        }

        LOGGER.info("Could not find compatible versions of the following mods:");
        for (Pair<ModManifest, ModManifest> pair : unsatisfiable) {
            ModManifest requiredBy = pair.getKey();
            ModManifest dep = pair.getRight();
            LOGGER.info(" {}({}) Requires Project {}({})", requiredBy.getName(), requiredBy.getId(), dep.getName(), dep.getId());
        }
    }

    public void install() throws ModInstallerException {
        tracker.nextStage(InstallStage.PREPARE);
        LOGGER.info("Installing {} mods.", toInstall.size());

        InstanceModifications modifications = instance.getOrCreateModifications();
        try {
            LOGGER.info("Preparing download tasks..");

            long totalSize = 0;
            List<Task<Void>> tasks = new ArrayList<>(toInstall.size());
            List<ModOverride> newOverrides = new ArrayList<>(toInstall.size());
            for (Pair<ModManifest, ModManifest.Version> toInstall : toInstall) {
                ModManifest mod = toInstall.getKey();
                ModManifest.Version version = toInstall.getValue();
                NewDownloadTask task = NewDownloadTask.builder()
                        .url(version.getUrl())
                        .dest(instance.getDir().resolve(version.getPath()).resolve(version.getName()))
                        .withValidation(version.createValidation().asDownloadValidation())
                        .withFileLocator(CreeperLauncher.localCache)
                        .build();
                if (!task.isRedundant()) {
                    long size = version.getSize();
                    if (size <= 0) {
                        size = NewDownloadTask.getContentLength(version.getUrl());
                    }
                    totalSize += size;
                    tasks.add((cancelToken, listener) -> {
                        try {
                            task.execute(cancelToken, listener);
                        } finally {
                            tracker.stepFinished();
                        }
                    });
                }

                newOverrides.add(new ModOverride(ModOverrideState.ADDED_ENABLED, version.getName(), version.getSha1(), mod.getId(), version.getId()));
            }

            LOGGER.info("Downloading mods..");

            tracker.nextStage(InstallStage.INSTALL);
            TaskProgressListener listener = tracker.listenerForStage();
            listener.start(totalSize);
            TaskProgressAggregator aggregator = new ParallelTaskProgressAggregator(listener);
            ParallelTaskHelper.executeInParallel(null, Task.TASK_POOL, tasks, aggregator);

            LOGGER.info("Finished downloading, Saving metadata changes.");

            instance.setModified(true);
            instance.saveJson();
            modifications.getOverrides().addAll(newOverrides);
            instance.saveModifications();

            LOGGER.info("Finished!");
        } catch (Throwable ex) {
            throw new ModInstallerException("Fatal error whilst installing mods.", ex);
        } finally {
            tracker.finished();
        }
    }

    public List<Pair<ModManifest, ModManifest.Version>> getToInstall() {
        return Collections.unmodifiableList(toInstall);
    }

    // @formatter:off
    @Override public String mcVersion() { return mcVersion; }
    @Override public String modLoader() { return modLoader; }
    @Override public void unavailableDependency(ModManifest requestedBy, ModManifest.Dependency dep) { unavailable.add(Pair.of(requestedBy, dep)); }
    @Override public void unsatisfiableDependency(ModManifest requestedBy, ModManifest dep) { unsatisfiable.add(Pair.of(requestedBy, dep)); }
    @Override public void addMod(ModManifest manifest, ModManifest.Version version) { toInstall.add(Pair.of(manifest, version)); }
    // @formatter:on

    private enum InstallStage implements OperationProgressTracker.Stage {
        RESOLVE,
        PREPARE,
        INSTALL,
    }

    public static class ModInstallerException extends Exception {

        public ModInstallerException(String message) {
            super(message);
        }

        public ModInstallerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
