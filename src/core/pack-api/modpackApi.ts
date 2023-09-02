import {ModpackApiModpackEndpoint} from '@/core/pack-api/endpoints/modpackApiModpackEndpoint';

export type StatusResult<T> = {
  status: "success" | "error" | "failure";
  message?: string;
} & T

class ModpackApi {
  private endpoints = {
    modpacks: new ModpackApiModpackEndpoint(),
    mods: ''
  }
  
  get modpacks() {
    return this.endpoints.modpacks;
  }
  
  get mods() {
    return this.endpoints.mods;
  }
}

export const modpackApi = new ModpackApi();