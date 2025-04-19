import missingArtSquare from '@/assets/images/ftb-missing-pack-art.webp';
import missingArtSplash from '@/assets/images/ftb-no-pack-splash-normal.webp';
import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import {SearchResultPack} from '@/core/types/modpacks/packSearch';
import { ModPack, PackProviders, Versions } from '@/core/types/appTypes.ts';
import { packBlacklist } from '@/store/modpackStore.ts';
import { useAppSettings } from '@/store/appSettingsStore.ts';

export type ArtworkTypes = "square" | "splash";
export type VersionTypes = "release" | "beta" | "alpha" | "archived" | "all" | "hotfix";

export const defaultArtwork: Record<ArtworkTypes, string> = {
  "square": missingArtSquare,
  "splash": missingArtSplash,
}

/**
 * Takes either a Modpack or an InstanceJson and attempts to work out the correct artwork to display.
 * 
 * @param packOrInstance the pack or instance to resolve the artwork for
 * @param artworkType the type of artwork to resolve
 * @param fallback a fallback pack to use if the instance can't find its own artwork
 * 
 * TODO: Support the new artwork system in the future
 */
export function resolveArtwork(packOrInstance: SugaredInstanceJson | InstanceJson | ModPack | SearchResultPack | null, artworkType: ArtworkTypes = "square", fallback?: ModPack | null) {
  const artwork = _resolveArtwork(packOrInstance, artworkType, fallback);
  return artwork.startsWith("http") ? encodeURI(artwork) : artwork;
}

function _resolveArtwork(packOrInstance: SugaredInstanceJson | InstanceJson | ModPack | SearchResultPack | null, artworkType: ArtworkTypes = "square", fallback?: ModPack | null) {
  if (!packOrInstance) {
    return defaultArtwork[artworkType];
  }

  // Splash is not stored locally for modpacks
  if ("uuid" in packOrInstance) {
    // It's an instance
    const instance = packOrInstance as SugaredInstanceJson;
    const fallbackArt = fallback?.art.find(e => e.type === artworkType)?.url ?? defaultArtwork[artworkType];
    if (instance.artworkFile === "") {
      return fallbackArt;
    }

    return artworkFileOrElse(instance, fallbackArt)
  }

  // It's a modpack
  const pack = packOrInstance as ModPack | SearchResultPack;
  return pack.art.find(e => e.type === artworkType)?.url ?? fallback?.art.find(e => e.type === artworkType)?.url ?? defaultArtwork[artworkType];
}

export function artworkFileOrElse(instance: SugaredInstanceJson, orElse: string = defaultArtwork["square"]) {
  if (instance.artworkFile) {
    return "ftb://load-pack-asset/" + instance.artworkFile + "?instancePath=" + instance.path;
  }
  
  return orElse;
}

const knownModloaders = [
  "vanilla",
  "neoforge",
  "fabric",
  "quilt",
  "forge",
]

export function resolveModloader(packOrInstance: SugaredInstanceJson | InstanceJson | ModPack | null) {
  if (!packOrInstance) {
    return "Forge";
  }
  
  if ("uuid" in packOrInstance) {
    const instance = packOrInstance as SugaredInstanceJson | InstanceJson;
    if (!instance.modLoader) {
      return "Forge";
    }
    
    // The modloader contains other information so we have to parse it for a known modloader
    const foundLoader = knownModloaders.find(e => instance.modLoader.toLowerCase().includes(e));
    if (foundLoader) {
      return foundLoader;
    }
    
    // For some reason the modloader will be set to the mc version so we have to support that
    if (instance.modLoader.includes(".")) {
      return "Vanilla";
    }
    
    return "Forge";
  }
  
  const pack = packOrInstance as ModPack;
  return pack.versions.at(0)?.targets.find(e => e.type === "modloader")?.name ?? "Forge";
}

export function resolveModLoaderVersion(instance: SugaredInstanceJson | InstanceJson | null) {
  if (!instance) {
    return "???";
  }
  
  const modloader = resolveModloader(instance).toLowerCase();
  const loaderFullVersion = instance.modLoader; 
  
  if (modloader === "vanilla") {
    return instance.mcVersion;
  }
  
  if (modloader === "neoforge") {
    const loaderParts = loaderFullVersion.split("-");
    return loaderParts.slice(1, 2)[0] ?? "???";
  }
  
  return loaderFullVersion.split("-").pop() ?? "???";
}

/**
 * Safely checks the type of a version to ensure it's a valid version to show the user
 */
export function isValidVersion(version: string, type: VersionTypes = "all") {
  // Not sure why you'd do this.
  if (type === "archived") {
    return false;
  }
  
  const safeVersion = version.toLowerCase();
  
  // Default behaviour is all of them
  if (type === "all") {
    return safeVersion !== "archived";
  }
  
  return safeVersion === type;
}

/**
 * Converts a number type to a string provider type
 */
export function typeIdToProvider(id: number): PackProviders {
  switch (id) {
    case 0: return "modpacksch";
    case 1: return "curseforge";
    default: return "modpacksch"
  }
}

export function sourceProviderToProvider(source: string): PackProviders {
  switch (source) {
    case "curseforge": return "curseforge";
    case "modpacks.ch": return "modpacksch";
    default: return "modpacksch";
  }
}

/**
 * Checks if an instance has an update available based on the users update channel. If the instance does not have its
 * own release channel set, it will use the apps default channel. If that channel is not available, it will use release.
 * 
 * Also, if we're a vanilla pack (81) or a modloader pack, or a custom pack (-1) don't offer updates.
 */
export function packUpdateAvailable(instance?: InstanceJson | SugaredInstanceJson | null, apiPack?: ModPack | null): Versions | undefined {
  if (!instance || !apiPack || !instance.versionId) {
    return undefined;
  }
  
  if (instance.id === -1 || packBlacklist.includes(instance.id)) {
    return undefined;
  }
  
  const appSettingsStore = useAppSettings();
  const channel = instance.releaseChannel !== "unset" ? instance.releaseChannel : (appSettingsStore.rootSettings?.instanceDefaults.updateChannel ?? "release");
  const allowedTypes: VersionTypes[] = channel === "release" ? ["release"] : (channel === "beta" ? ["release", "beta"] : ["release", "beta", "alpha"]);
  
  const packVersions = apiPack.versions.sort((a, b) => b.id - a.id);
  
  let versions = packVersions.filter(e => allowedTypes.includes(e.type.toLowerCase() as VersionTypes));
  if (versions.length === 0 && channel === "alpha") {
    return undefined;
  }
  
  // Relax the version type requirements if the user selected channel is not available
  if (versions.length === 0) {
    versions = packVersions;
  }
  
  const latestVersion = versions[0];
  if (!latestVersion) {
    return undefined;
  }
  
  if (latestVersion.id > instance.versionId) {
    return latestVersion;
  }
  
  return undefined;
}

export function compatibleCrossLoaderPlatforms(mcVersion: string, loader: string) {
  // fabric and quilt are forever compatible (so far)
  if (loader === "fabric") {
    return ["fabric", "quilt"];
  }
  
  // Forge and NeoForge are compatible with each other during this version range
  if ((loader === "forge" || loader === "neoforge") && (mcVersion == "1.20.1" || mcVersion === "1.20")) {
    return ["forge", "neoforge"];
  }
  
  // Otherwise, default to the given input as there is no special handling
  return [loader];
}