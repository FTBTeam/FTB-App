package net.creeperhost.creeperlauncher.instance.importer.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoader;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public record CurseForgeMeta(
    BaseModLoader baseModLoader,
    Boolean isUnlocked,
    String javaArgsOverride, // This might be an array
    String lastPlayed,
    Long playedCount,
    Manifest manifest,
    String fileDate,
    @Nullable InstalledModpack installedModpack,
    @JsonProperty("projectID")
    Long projectId,
    @JsonProperty("fileID")
    Long fileId,
    @Nullable String customAuthor,
    List<?> modpackOverrides,
    Boolean isMemoryOverride,
    Long allocatedMemory,
    @Nullable Path profileImagePath,
    Boolean isVanilla,
    String guid,
    @JsonProperty("gameTypeID")
    Long gameTypeId,
    String installPath,
    String name,
    List<?> cachedScans,
    Boolean isValid,
    String lastPreviousMatchUpdate,
    String lastRefreshAttempt,
    Boolean isEnabled,
    String gameVersion,
    @Nullable String gameVersionFlavor,
    @Nullable Number gameVersionTypeId,
    Boolean preferenceAlternateFile,
    Boolean preferenceAutoInstallUpdates,
    Boolean preferenceQuickDeleteLibraries,
    Boolean preferenceDeleteSavedVariables,
    Boolean preferenceProcessFileCommands,
    Long preferenceReleaseType,
    @Nullable Path preferenceModdingFolderPath,
    SyncProfile syncProfile,
    String installDate,
    List<InstalledAddon> installedAddons,
    Boolean wasNameManuallyChanged
) {
    record BaseModLoader(
        String forgeVersion,
        String name,
        Long type,
        String downloadUrl,
        String filename,
        Long installMethod,
        Boolean latest,
        Boolean recommended,
        String versionJson,
        String librariesInstallLocation,
        String minecraftVersion,
        String installProfileJson
    ) {}
    
    public record InstalledModpack(
        @JsonProperty("instanceID") String instanceId,
        Long modSource,
        @JsonProperty("addonID") Long addonId,
        @JsonProperty("gameID") Long gameId,
        @JsonProperty("categoryClassID") Long categoryClassId,
        @JsonProperty("gameInstanceID") String gameInstanceId,
        String name,
        @Nullable Path modFolderPath,
        String fileNameOnDisk,
        List<Author> authors,
        String primaryAuthor,
        Long primaryCategoryId,
        Long packageType,
        @JsonProperty("webSiteURL") String webSiteUrl,
        String thumbnailUrl,
        List<?> tags,
        InstalledFile installedFile,
        String dateInstalled,
        String dateUpdated,
        String dateLastUpdateAttempted,
        Long status,
        Long installSource,
        @Nullable String preferenceReleaseType,
        @Nullable Boolean preferenceAutoInstallUpdates,
        Boolean preferenceAlternateFile,
        Boolean preferenceIsIgnored,
        Boolean isModified,
        Boolean isWorkingCopy,
        Boolean isFuzzyMatch,
        @Nullable String manifestName,
        List<?> installedTargets,
        InstalledFile latestFile
    ) {}
    
    public record Manifest(
        Minecraft minecraft,
        String manifestType,
        Long manifestVersion,
        String name,
        String version,
        String author,
        @Nullable String description,
        @JsonProperty("projectID")
        @Nullable Number projectId,
        List<File> files,
        String overrides
    ) {}
    
    public record Minecraft(
        String version,
        String additionalJavaArgs,
        List<ModLoader> modLoaders,
        Object libraries // No clue?  
    ) {}
    
    public record SyncProfile(
        @JsonProperty("PreferenceEnabled") Boolean preferenceEnabled,
        @JsonProperty("PreferenceAutoSync") Boolean preferenceAutoSync,
        @JsonProperty("PreferenceAutoDelete") Boolean preferenceAutoDelete,
        @JsonProperty("PreferenceBackupSavedVariables") Boolean preferenceBackupSavedVariables,
        @JsonProperty("GameInstanceGuid") String gameInstanceGuid,
        @JsonProperty("SyncProfileID") Long syncProfileId,
        @JsonProperty("SavedVariablesProfile") @Nullable Object savedVariablesProfile, // No clue?
        @JsonProperty("LastSyncDate") String lastSyncDate
    ) {}
    
    public record Author(
        @JsonProperty("Name") String name
    ) {}
    
    public record InstalledAddon(
        @JsonProperty("instanceID") String instanceId,
        Long modSource,
        @JsonProperty("addonID") Long addonId,
        @JsonProperty("gameID") Long gameId,
        @JsonProperty("categoryClassID") Long categoryClassId,
        @JsonProperty("gameInstanceID") String gameInstanceId,
        String name,
        @Nullable Path modFolderPath,
        String fileNameOnDisk,
        List<Author> authors,
        String primaryAuthor,
        Long primaryCategoryId,
        Long packageType,
        @JsonProperty("webSiteURL") String webSiteUrl,
        String thumbnailUrl,
        List<?> tags,
        InstalledFile installedFile,
        String dateInstalled,
        String dateUpdated,
        String dateLastUpdateAttempted,
        Long status,
        Long installSource,
        @Nullable String preferenceReleaseType,
        @Nullable Boolean preferenceAutoInstallUpdates,
        Boolean preferenceAlternateFile,
        Boolean preferenceIsIgnored,
        Boolean isModified,
        Boolean isWorkingCopy,
        Boolean isFuzzyMatch,
        @Nullable String manifestName,
        List<?> installedTargets,
        InstalledFile latestFile
    ) {}
    
    public record InstalledFile(
        Long id,
        String fileName,
        String fileDate,
        Long fileLength,
        Long releaseType,
        Long fileStatus,
        String downloadUrl,
        Boolean isAlternate,
        Long alternateFileId,
        List<?> dependencies,
        Boolean isAvailable,
        List<Module> modules,
        Long packageFingerprint,
        List<String> gameVersion,
        List<SortableGameVersion> sortableGameVersion,
        Boolean hasInstallScript,
        Boolean isCompatibleWithClient,
        Boolean isEarlyAccessContent,
        Long restrictProjectFileAccess,
        Long projectStatus,
        Long projectId,
        String fileNameOnDisk,
        @JsonProperty("Hashes")
        List<Hash> hashes
    ) {}
    
    public record Hash(
        @JsonProperty("Value") String value
    ) {}
    
    public record SortableGameVersion(
        String gameVersion,
        String gameVersionName,
        Long gameVersionTypeId
    ) {}
}
