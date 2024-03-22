import {ModpackApiModpackEndpoint} from '@/core/pack-api/endpoints/modpackApiModpackEndpoint';
import {ModpacksApiSearchEndpoint} from '@/core/pack-api/endpoints/modpacksApiSearchEndpoint';
import {ModpacksApiBrowseEndpoint} from '@/core/pack-api/endpoints/modpacksApiBrowseEndpoint';

export type StatusResult<T> = {
  status: "success" | "error" | "failure";
  message?: string;
} & T

class ModpackApi {
  private endpoints = {
    modpacks: new ModpackApiModpackEndpoint(),
    mods: '',
    search: new ModpacksApiSearchEndpoint(),
    browse: new ModpacksApiBrowseEndpoint(),
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
  
  get browse() {
    return this.endpoints.browse;
  }
}

export const modpackApi = new ModpackApi();