import {ApiEndpoint} from '@/core/pack-api/endpoints/apiEndpoint';
import {StatusResult} from '@/core/pack-api/modpackApi';
import {ModPack} from '@/modules/modpacks/types';

export class ModpackApiModpackEndpoint extends ApiEndpoint {
  constructor() {
    super("modpack");
  }
  
  async getModpacks() {
    return this.fetchPrivate<StatusResult<{ 
      packs: number[]
    }>>("GET", "modpacks");
  }
  
  async getModpack(id: number) {
    return this.fetchPrivate<StatusResult<ModPack>>("GET", id.toString());
  }
}