import {ModpackState} from '@/core/state/modpacks/modpacksState';
import {InstanceState} from '@/core/state/instances/instancesState';
import {InstallState} from '@/core/state/instances/installState';
import {NewsState} from '@/core/state/misc/newsState';
import {DialogsState} from '@/core/state/misc/dialogsState';

export type AppState = {
  "v2/modpacks": ModpackState,
  "v2/instances": InstanceState,
  "v2/install": InstallState,
  "v2/news": NewsState,
  "v2/dialogs": DialogsState,
}

export type AppStoreModules = keyof AppState;

export function ns(module: AppStoreModules) {
  return { namespace: module }
}