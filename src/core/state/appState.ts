import {Module } from "vuex"
import {ModpackState, modpackStateModule} from '@/core/state/modpacks/modpacksState';
import {InstanceState, instanceStateModule} from '@/core/state/instances/instancesState';
import {InstallState, installStateModule} from '@/core/state/instances/installState';
import {RootState} from '@/types';
import {NewsState, newsStateModule} from '@/core/state/misc/newsState';

export type AppState = {
  "v2/modpacks": ModpackState,
  "v2/instances": InstanceState,
  "v2/install": InstallState,
  "v2/news": NewsState
}

export type AppStoreModules = keyof AppState;

export function ns(module: AppStoreModules) {
  return { namespace: module }
}

export const appStateStore: Module<AppState, RootState> = {
  modules: {
    "v2/modpacks": modpackStateModule,
    "v2/instances": instanceStateModule,
    "v2/install": installStateModule,
    "v2/news": newsStateModule,
  }  
}