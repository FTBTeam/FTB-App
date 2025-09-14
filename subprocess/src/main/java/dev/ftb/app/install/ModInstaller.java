package dev.ftb.app.install;

import com.google.gson.JsonParseException;
import dev.ftb.app.AppMain;
import dev.ftb.app.data.InstanceModifications;
import dev.ftb.app.data.InstanceModifications.ModOverride;
import dev.ftb.app.data.InstanceModifications.ModOverrideState;
import dev.ftb.app.data.mod.CurseMetadata;
import dev.ftb.app.data.mod.ModInfo;
import dev.ftb.app.data.mod.ModManifest;
import dev.ftb.app.install.tasks.*;
import dev.ftb.app.pack.Instance;
import net.covers1624.quack.collection.FastStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static dev.ftb.app.data.InstanceModifications.ModOverrideState.*;

public class ModInstaller implements ModCollector {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Instance instance;

    private final String mcVersion;
    private final String modLoader;

    private final long modId;
    private final long versionId;

    private final OperationProgressTracker tracker;

    private final List<ModInfo> existingMods = new ArrayList<>();
    private final List<Pair<ModManifest, ModManifest.Dependency>> unavailable = new ArrayList<>();
    private final List<Pair<ModManifest, ModManifest>> unsatisfiable = new ArrayList<>();
    private final List<Pair<ModManifest, ModManifest.Version>> toInstall = new ArrayList<>();

    public ModInstaller(Instance instance, String mcVersion, String modLoader, long modId, long versionId) {
        this.instance = instance;
        this.mcVersion = mcVersion;
        this.modLoader = modLoader;

        this.modId = modId;
        this.versionId = versionId;

        tracker = new OperationProgressTracker(
                "mod_install",
                Map.of(
                        "instance", instance.getUuid().toString(),
                        "mod_id", String.valueOf(modId),
                        "version_id", String.valueOf(versionId)
                )
        );
    }

    public void resolve() throws ModInstallerException {
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
        existingMods.addAll(instance.getMods(true));
        LOGGER.info("Filtering found dependencies from existing mod list.");
        toInstall.removeIf(pair -> {
            ModManifest manifest = pair.getKey();
            ModManifest.Version version = pair.getValue();
            if (manifest.getId() == modId) return false;

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
            List<Task> tasks = new ArrayList<>(toInstall.size());
            List<ModOverride> overrides = modifications.getOverrides();
            List<ModOverride> oldOverrides = new ArrayList<>();
            List<ModOverride> newOverrides = new ArrayList<>(toInstall.size());
            for (Pair<ModManifest, ModManifest.Version> toInstall : toInstall) {
                ModManifest mod = toInstall.getKey();
                ModManifest.Version version = toInstall.getValue();
                ModInfo selfExisting = FastStream.of(existingMods)
                        .filter(e -> e.curse() != null && e.curse().curseProject() == modId)
                        .onlyOrDefault();
                String suffix = selfExisting != null && !selfExisting.enabled() ? ".disabled" : "";
                DownloadTask task = DownloadTask.builder()
                        .url(version.getUrl())
                        .dest(instance.getDir().resolve(version.getPath()).resolve(version.getName() + suffix))
                        .withValidation(version.createValidation().asDownloadValidation())
                        .withFileLocator(AppMain.localCache)
                        .build();
                if (!task.isRedundant()) {
                    long size = version.getSize();
                    if (size <= 0) {
                        size = DownloadTask.getContentLength(version.getUrl());
                    }
                    totalSize += size;
                    tasks.add((cancelToken, listener) -> {
                        try {
                            task.execute(cancelToken, listener);
                            // Delete old file if we are updating.
                            if (selfExisting != null) {
                                String fName = selfExisting.fileName();
                                if (!selfExisting.enabled()) {
                                    fName += ".disabled";
                                } else {
                                    fName = StringUtils.removeEnd(fName, ".disabled");
                                }
                                Files.deleteIfExists(instance.getDir().resolve(version.getPath()).resolve(fName));
                            }
                        } finally {
                            tracker.stepFinished();
                        }
                    });
                }

                if (selfExisting != null) {
                    ModOverride existingOverride = FastStream.of(overrides)
                            .filter(e -> e.getCurseProject() == modId && e.getId() == selfExisting.fileId())
                            .firstOrDefault();
                    if (existingOverride != null) {
                        oldOverrides.add(existingOverride);
                        ModOverrideState state = existingOverride.getState();
                        ModOverrideState newState = switch(state) {
                            case ENABLED -> UPDATED_ENABLED;
                            case DISABLED -> UPDATED_DISABLED;
                            default -> state;
                        };
                        newOverrides.add(new ModOverride(newState, version.getName(), selfExisting.fileId(), version.getSha1(), version.getMurmur(), mod.getId(), version.getId()));
                    } else {
                        if (selfExisting.fileId() != -1) {
                            newOverrides.add(new ModOverride(
                                    selfExisting.enabled() ? UPDATED_ENABLED : UPDATED_DISABLED,
                                    version.getName(),
                                    selfExisting.fileId(),
                                    version.getSha1(),
                                    version.getMurmur(),
                                    mod.getId(),
                                    version.getId()
                            ));
                        } else {
                            // What?
                        }
                    }
                } else {
                    newOverrides.add(new ModOverride(ADDED_ENABLED, version.getName(), version.getSha1(), version.getMurmur(), mod.getId(), version.getId()));
                }
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
            overrides.removeAll(oldOverrides);
            overrides.addAll(newOverrides);
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
