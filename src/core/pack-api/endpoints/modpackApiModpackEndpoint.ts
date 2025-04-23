import {ApiEndpoint} from '@/core/pack-api/endpoints/apiEndpoint';
import {StatusResult} from '@/core/pack-api/modpackApi';
import { ModPack, ModpackVersion, PackProviders } from '@/core/types/appTypes.ts';

type PackIdList = {
  packs: number[];
  total: number;
  limit: number;
  refreshed: number;
}

export class ModpackApiModpackEndpoint extends ApiEndpoint {
  constructor() {
    super("modpack");
  }
  
  async getModpacks() {
    return this.fetchPrivate<StatusResult<PackIdList>>("GET", "all");
  }
  
  async getPrivatePacks() {
    return this.fetchPrivate<StatusResult<PackIdList>>("GET", "private/100");
  }
  
  async getModpack(id: number | string, provider: PackProviders = "modpacksch") {
    if (provider === "modpacksch") {
      return this.fetchPrivate<StatusResult<ModPack>>("GET", "" + id);
    }
    
    // Use curseforge for other providers
    return this.fetchPrivate<StatusResult<ModPack>>("GET", "" + id, "curseforge");
  }
  
  async getModpackVersion(id: number, versionId: number, provider: PackProviders = "modpacksch") {
    if (provider === "modpacksch") {
      return this.fetchPrivate<StatusResult<ModpackVersion>>("GET", `${id}/${versionId}`);
    }
    
    // Use curseforge for other providers
    return this.fetchPrivate<StatusResult<ModpackVersion>>("GET", `${id}/${versionId}`, "curseforge");
  }
  
  async getChangelog(packId: number, versionId: number, provider: PackProviders = "modpacksch") {
    if (provider === "modpacksch") {
      return this.fetchPrivate<StatusResult<{ content: string }>>("GET", `${packId}/${versionId}/changelog`);
    }
    
    // Use curseforge for other providers
    return this.fetchPrivate<StatusResult<{ content: string }>>("GET", `${packId}/${versionId}/changelog`, "curseforge");
  }
  
  async getFeaturedPacks(limit = 5) {
    return this.fetchPublic<PackIdList>("GET", `featured/${limit}`)
  }
  
  async getLatestPacks(limit = 5) {
    return this.fetchPublic<PackIdList>("GET", `updated/${limit}`)
  }
}