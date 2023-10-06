import {ModPack, PackProviders} from '@/modules/modpacks/types';

import missingArtSquare from '@/assets/images/ftb-missing-pack-art.webp';
import missingArtSplash from '@/assets/images/ftb-no-pack-splash-normal.webp';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {SearchResultPack} from '@/core/@types/modpacks/packSearch';

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
    if (instance.art === "") {
      return fallbackArt;
    }

    return instance.art ?? fallbackArt;
  }

  // It's a modpack
  const pack = packOrInstance as ModPack | SearchResultPack;
  return pack.art.find(e => e.type === artworkType)?.url ?? fallback?.art.find(e => e.type === artworkType)?.url ?? defaultArtwork[artworkType];
}

const knownModloaders = [
  "forge",
  "fabric",
  "quilt",
  "neoforge"
]

export function resolveModloader(packOrInstance: SugaredInstanceJson | ModPack | null) {
  if (!packOrInstance) {
    return "Forge";
  }
  
  if ("uuid" in packOrInstance) {
    const instance = packOrInstance as SugaredInstanceJson;
    if (!instance.modLoader) {
      return "Forge";
    }
    
    return knownModloaders.find(e => instance.modLoader.toLowerCase().includes(e)) ?? "Forge";
  }
  
  const pack = packOrInstance as ModPack;
  return pack.versions.at(0)?.targets.find(e => e.type === "modloader")?.name ?? "Forge";
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