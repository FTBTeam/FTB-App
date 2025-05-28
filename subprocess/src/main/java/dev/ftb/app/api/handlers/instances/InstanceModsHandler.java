package dev.ftb.app.api.handlers.instances;

import com.google.gson.Gson;
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
import dev.ftb.app.data.modpack.ModsLookupManifest;
import dev.ftb.app.pack.Instance;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
        
        pollMods(data, instance, mods, ml, mcVersion)
            .exceptionally((e) -> {
                LOGGER.error("Failed to poll mod data for instance: {}", instance.getName(), e);
                return null;
            });
    }

    private static CompletableFuture<Void> pollMods(InstanceModsData data, Instance instance, List<ModInfo> mods, @Nullable ModpackVersionManifest.Target modLoader, @Nullable String mcVersion) {
        return CompletableFuture.completedFuture(mods)
                .thenApplyAsync(modsList -> {
                    var modsManifest = instance.getModsManifest();
                    var knownFileNames = modsManifest != null ? modsManifest.getMods().stream().map(ModpackVersionModsManifest.Mod::getFilename).toList() : List.of();
                    
                    var allHashes = mods.stream()
                        .filter(e -> !knownFileNames.contains(e.fileName()))
                        .map(ModInfo::murmurHash)
                        .filter(Objects::nonNull) // This shouldn't happen, but just in case.
                        .filter(e -> !e.equals("-1")) // -1 is a placeholder for no hash.
                        .toList();
                    
                    List<ModpackVersionModsManifest.Mod> resolvedMods = new ArrayList<>();
                    if (modsManifest != null) {
                        resolvedMods.addAll(modsManifest.getMods());
                    }

                    System.out.println("Looking up mod hashes: " + allHashes.size());
                    System.out.println("Out of " + mods.size() + " mods, " + knownFileNames.size() + " are known, and " + allHashes.size() + " have hashes.");
                    
                    if (!allHashes.isEmpty()) {
                        var client = Constants.httpClient();
                        var request = client.newCall(new Request.Builder()
                            .url(Constants.FTB_MODPACKS_API + "/mod/lookup/hashes")
                            .post(RequestBody.create(new Gson().toJson(Map.of("hashes", allHashes)), MediaType.parse("application/json")))
                            .build());

                        try (var response = request.execute()) {
                            var responseBody = response.body();
                            if (!response.isSuccessful() || responseBody == null) {
                                LOGGER.error("Failed to lookup mod hashes: {}", response.message());
                                return null;
                            }
                            
                            var responseJson = new Gson().fromJson(responseBody.string(), ModsLookupManifest.class);
                            resolvedMods.addAll(responseJson.data().stream().map(ModpackVersionModsManifest.Mod::fromLookupResponse).toList());
                        } catch (Exception e) {
                            LOGGER.error("Failed to create HTTP client for mod data lookup.", e);
                        }
                    }

                    var richModData = mods.stream()
                        .map(e -> Pair.of(
                            e,
                            resolvedMods.stream().filter(a -> a.getFilename().equals(e.fileName())).findFirst()
                        ))
                        .map(pair -> new InstanceModsData.RichModDataBinding(
                            pair.getKey(),
                            pair.getValue().map(e -> CurseMetadata.full(
                                e.getCurseProject(),
                                e.getCurseFile(),
                                e.getName(),
                                e.getCurseSlug(),
                                e.getSynopsis(),
                                e.getIcon()
                            )).orElse(null))
                        )
                        .toList();

                    WebSocketHandler.sendMessage(new InstanceModsData.AllRichModData(data, richModData));
                    
                    return richModData.stream().filter(e -> e.richData() != null).toList();
                })
                .thenAccept((richDataMods) -> {
                    // We have no curse data or no ModLoader or no Game version, update checking is not possible.
                    if (modLoader == null || mcVersion == null) return;

                    for (var mod : richDataMods) {
                        if (mod.richData() == null) continue; // not possible, stopping ide complaints
                        ModManifest manifest = Constants.MOD_VERSION_CACHE.queryMod(mod.richData().curseProject()).join();
                        if (manifest == null) continue;

                        var version = manifest.findLatestCompatibleVersion(modLoader.getName(), mcVersion);
                        if (version == null) continue;

                        if (version.getId() <= mod.richData().curseFile()) continue;
                        WebSocketHandler.sendMessage(new InstanceModsData.UpdateAvailable(
                            data,
                            mod.file(),
                            CurseMetadata.full(
                                manifest.getId(),
                                version.getId(),
                                version.getName(),
                                mod.richData().slug(),
                                mod.richData().synopsis(),
                                mod.richData().icon()
                            )
                        ));
                    }
                })
                .thenRunAsync(() -> 
                    WebSocketHandler.sendMessage(new InstanceModsData.UpdateCheckingFinished(data)
                ), AppMain.taskExeggutor);
    }
    
//    private static @Nullable CurseMetadata lookupCurseData(InstanceModsData data, @Nullable CurseMetadata meta, CompletableFuture<ModpackVersionModsManifest> modsManifestFuture, ModInfo mod) {
//        if (meta != null && meta.type() == CurseMetadata.Type.FULL) return meta;
//
//        if (meta == null) {
//            if (mod.fileId() > 0) {
//                ModpackVersionModsManifest modsManifest = modsManifestFuture.join();
//                ModpackVersionModsManifest.Mod modManifest = modsManifest != null ? modsManifest.getMod(mod.fileId()) : null;
//                meta = Constants.CURSE_METADATA_CACHE.getCurseMeta(modManifest, Objects.requireNonNullElse(mod.murmurHash(), "-1"));
//            } else {
//                // TODO: We could in theory lookup the sha1, but I don't think we can actually get into this state in normal operation.
//                return null;
//            }
//        } else {
//            meta = Constants.CURSE_METADATA_CACHE.getCurseMeta(meta.curseProject(), meta.curseFile());
//        }
//        WebSocketHandler.sendMessage(new InstanceModsData.RichModData(data, mod, meta));
//        return meta;
//    }
}
