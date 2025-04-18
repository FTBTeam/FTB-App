package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.Constants;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceModsData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.InstanceModifications;
import dev.ftb.app.data.mod.CurseMetadata;
import dev.ftb.app.data.mod.ModInfo;
import dev.ftb.app.data.mod.ModManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.data.modpack.ModpackVersionModsManifest;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class InstanceModsHandler implements IMessageHandler<InstanceModsData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceModsData data) {
        var instance = Instances.getInstance(data.uuid);
        if (instance == null) return;

        var mods = instance.getMods(false);
        WebSocketHandler.sendMessage(new InstanceModsData.Reply(data, mods));

        pollModData(data, instance, mods);
    }

    private static void pollModData(InstanceModsData data, Instance instance, List<ModInfo> mods) {
        var mcVersion = instance.getMcVersion();

        InstanceModifications modifications = instance.getModifications();
        ModpackVersionManifest.Target ml = modifications != null ? modifications.getModLoaderOverride() : null;
        if (ml == null) {
            ml = instance.versionManifest.findTarget("modloader");
        }

        CompletableFuture<ModpackVersionModsManifest> modsManifestFuture = CompletableFuture.supplyAsync(instance::getModsManifest, AppMain.taskExeggutor);

        var modLoader = ml;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var mod : mods) {
            CompletableFuture<Void> future = pollMod(data, modsManifestFuture, mod, modLoader, mcVersion);
            if (future != null) {
                futures.add(future);
            }
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRunAsync(() -> WebSocketHandler.sendMessage(new InstanceModsData.UpdateCheckingFinished(data)), AppMain.taskExeggutor);
    }

    private static CompletableFuture<Void> pollMod(InstanceModsData data, CompletableFuture<ModpackVersionModsManifest> modsManifestFuture, ModInfo mod, @Nullable ModpackVersionManifest.Target modLoader, @Nullable String mcVersion) {
        // Start with whatever data we have
        return CompletableFuture.completedFuture(mod.curse())
                // Try and lookup rich data, updating the UI if we find any
                .thenApplyAsync(curseData -> lookupCurseData(data, curseData, modsManifestFuture, mod), AppMain.taskExeggutor)
                // Then try and check for updates.
                .thenAccept(curseData -> {
                    // We have no curse data or no ModLoader or no Game version, update checking is not possible.
                    if (curseData == null || modLoader == null || mcVersion == null) return;

                    ModManifest manifest = Constants.MOD_VERSION_CACHE.queryMod(curseData.curseProject()).join();
                    if (manifest == null) return;

                    var version = manifest.findLatestCompatibleVersion(modLoader.getName(), mcVersion);
                    if (version == null) return;

                    if (version.getId() <= curseData.curseFile()) return;
                    WebSocketHandler.sendMessage(new InstanceModsData.UpdateAvailable(
                            data,
                            mod,
                            CurseMetadata.full(
                                    manifest.getId(),
                                    version.getId(),
                                    version.getName(),
                                    curseData.slug(),
                                    curseData.synopsis(),
                                    curseData.icon()
                            )
                    ));
                });
    }

    private static @Nullable CurseMetadata lookupCurseData(InstanceModsData data, @Nullable CurseMetadata meta, CompletableFuture<ModpackVersionModsManifest> modsManifestFuture, ModInfo mod) {
        if (meta != null && meta.type() == CurseMetadata.Type.FULL) return meta;

        if (meta == null) {
            if (mod.fileId() > 0) {
                ModpackVersionModsManifest modsManifest = modsManifestFuture.join();
                ModpackVersionModsManifest.Mod modManifest = modsManifest != null ? modsManifest.getMod(mod.fileId()) : null;
                meta = Constants.CURSE_METADATA_CACHE.getCurseMeta(modManifest, Objects.requireNonNullElse(mod.murmurHash(), "-1"));
            } else {
                // TODO: We could in theory lookup the sha1, but I don't think we can actually get into this state in normal operation.
                return null;
            }
        } else {
            meta = Constants.CURSE_METADATA_CACHE.getCurseMeta(meta.curseProject(), meta.curseFile());
        }
        WebSocketHandler.sendMessage(new InstanceModsData.RichModData(data, mod, meta));
        return meta;
    }
}
