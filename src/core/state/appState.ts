import {ModpackState} from '@/core/state/modpacks/modpacksState';
import {InstanceState} from '@/core/state/instances/instancesState';
import {InstallState} from '@/core/state/instances/installState';
import {NewsState} from '@/core/state/misc/newsState';
import {DialogsState} from '@/core/state/misc/dialogsState';
import {CoreAppState} from '@/core/state/core/coreAppState';
import {MTAuthState} from '@/core/state/core/mtAuthState';
import {ApiCredentialsState} from '@/core/state/core/apiCredentialsState';
import {UserFavouritesState} from '@/core/state/misc/userFavouritesState';
import {RunningState} from '@/core/state/misc/runningState';
import {CoreState} from '@/modules/core/core.types';

export type AppState = {
  "core": CoreState,
  "v2/app": CoreAppState,
  "v2/modpacks": ModpackState,
  "v2/instances": InstanceState,
  "v2/install": InstallState,
  "v2/news": NewsState,
  "v2/dialogs": DialogsState,
  "v2/mtauth": MTAuthState
  "v2/apiCredentials": ApiCredentialsState,
  "v2/userFavourites": UserFavouritesState,
  "v2/running": RunningState
}

export type AppStoreModules = keyof AppState;

export function ns(module: AppStoreModules) {
  return { namespace: module }
}

export function nsAction(module: AppStoreModules, action: string) {
  return `${module}/${action}`
}
