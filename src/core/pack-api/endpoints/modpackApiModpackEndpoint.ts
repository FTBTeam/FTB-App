import {ApiEndpoint} from '@/core/pack-api/endpoints/apiEndpoint';
import {StatusResult} from '@/core/pack-api/modpackApi';
import {ModPack} from '@/modules/modpacks/types';

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

  /**
   * @deprecated I don't recommend using this.
   */
  async getModpacks() {
    return this.fetchPrivate<StatusResult<PackIdList>>("GET", "modpacks");
  }
  
  async getModpack(id: number) {
    return this.fetchPrivate<StatusResult<ModPack>>("GET", id.toString());
  }
  
  async getFeaturedPacks(limit = 5) {
    return this.fetchPublic<PackIdList>("GET", `featured/${limit}`)
  }
  
  async getLatestPacks(limit = 5) {
    return this.fetchPublic<PackIdList>("GET", `updated/${limit}`)
  }
}