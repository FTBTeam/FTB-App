import { defineStore } from 'pinia';
import { ModPack, ModpackVersion, PackProviders } from '@/modules/modpacks/types.ts';
import { createLogger } from '@/core/logger.ts';
import { modpackApi } from '@/core/pack-api/modpackApi.ts';

const logger = createLogger("modpacks/modpacksState.ts");

export const packBlacklist = [
  81,  // Vanilla
  104, // Forge
  105, // Fabric
  116  // NeoForge
]

export const loaderMap = new Map([
  [81, "Vanilla"],
  [104, "Forge"],
  [105, "Fabric"],
  [116, "NeoForge"]
])

type ModpackState = {
  modpacks: Record<number, ModPack>;
  modpackVersions: Record<number, ModpackVersion>;
  featuredPackIds: number[];
  latestPackIds: number[];
}

export const useModpackStore = defineStore("modpack", {
  state: (): ModpackState => {
    return {
      modpacks: {},
      modpackVersions: {},
      featuredPackIds: [],
      latestPackIds: [],
    }
  },

  actions: {
    async getModpack(id: number, provider: PackProviders = "modpacksch") {
      if (id === -1) {
        return null;
      }
      
      if (this.modpacks[id]) {
        return this.modpacks[id];
      }

      const req = await modpackApi.modpacks.getModpack(id, provider);
      if (req == null || req.status !== "success") {
        logger.debug(`Failed to get modpack ${id}`)
        return null;
      }

      const modpack = req as ModPack
      this.modpacks[id] = modpack; // Cache modpack
      return modpack;
    },
    
    async getVersion(id: number, version: number, provider: PackProviders = "modpacksch") {
      if (id === -1) {
        return null;
      }

      if (this.modpackVersions[version]) {
        return this.modpackVersions[version];
      }

      const req = await modpackApi.modpacks.getModpackVersion(id, version, provider);
      if (req == null || req.status !== "success") {
        logger.debug(`Failed to get modpack ${id} version ${version}`)
        return null;
      }

      const modpackVersion = req
      this.modpackVersions[version] = modpackVersion;
      return modpackVersion;
    },
    
    async getFeaturedPacks() {
      this.featuredPackIds = await getModpackIds("featured", this.featuredPackIds);
    },
    
    async getLatestModpacks() {
      this.latestPackIds = await getModpackIds("latest", this.latestPackIds, 15);
    },
    
    async clearModpacks() {
      this.modpacks = {};
      this.modpackVersions = {};
      this.featuredPackIds = [];
      this.latestPackIds = [];
    }
  }
})

async function getModpackIds(type: "featured" | "latest", existing: number[], limit = 5, ignoreBlacklist = false): Promise<number[]> {
  const endpoints: Record<"featured" | "latest", () => Promise<any>> = {
    featured: async () => modpackApi.modpacks.getFeaturedPacks(limit),
    latest: async () => modpackApi.modpacks.getLatestPacks(limit),
  }

  const endpoint = endpoints[type];
  if (existing.length > 0) {
    return existing;
  }

  try {
    const req = await endpoint();
    if (!req) {
      logger.error(`Failed to get ${type} packs`)
      return [];
    }

    let featuredPacks = req.packs.sort((a: number, b: number) => b - a)
    if (!ignoreBlacklist) {
      featuredPacks = featuredPacks.filter((e: number) => !packBlacklist.includes(e));
    }
    
    return featuredPacks;
  } catch (e) {
    logger.error(`Failed to get ${type} packs`, e)
  }

  return [];
}