import {ModpackApiModpackEndpoint} from '@/core/pack-api/endpoints/modpackApiModpackEndpoint';
import {ModpacksApiSearchEndpoint} from '@/core/pack-api/endpoints/modpacksApiSearchEndpoint';

export type StatusResult<T> = {
  status: "success" | "error" | "failure";
  message?: string;
} & T

class ModpackApi {
  private endpoints = {
    modpacks: new ModpackApiModpackEndpoint(),
    mods: '',
    search: new ModpacksApiSearchEndpoint()
  }
  
  get modpacks() {
    return this.endpoints.modpacks;
  }
  
  get mods() {
    return this.endpoints.mods;
  }
  
  get search() {
    return this.endpoints.search;
  }
}

export const modpackApi = new ModpackApi();