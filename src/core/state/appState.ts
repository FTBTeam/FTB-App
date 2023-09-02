import {Store, StoreOptions } from "vuex"
import {modpackStateModule} from '@/core/state/modpacks/modpacksState';
import {instanceStateModule} from '@/core/state/instances/instancesState';

export type AppState = {
}

export enum AppStoreModules {
  modpacks = 'v2/modpacks',
  instances = 'v2/instances',
}

export function ns(module: AppStoreModules) {
  return { namespace: module }
}

export const appStateStore: StoreOptions<AppState> = {
  modules: {
    "v2/modpacks": modpackStateModule,
    "v2/instances": instanceStateModule,
  }  
}