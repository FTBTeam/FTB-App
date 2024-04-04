import {ModpackState} from '@/core/state/modpacks/modpacksState';
import {InstanceState} from '@/core/state/instances/instancesState';
import {InstallState} from '@/core/state/instances/installState';
import {NewsState} from '@/core/state/misc/newsState';
import {DialogsState} from '@/core/state/misc/dialogsState';
import {AdsState} from '@/core/state/misc/adsState';
import {CoreAppState} from '@/core/state/core/coreAppState';
import {MTAuthState} from '@/core/state/core/mtAuthState';
import {ApiCredentialsState} from '@/core/state/core/apiCredentialsState';
import {UserFavouritesState} from '@/core/state/misc/userFavouritesState';

export type AppState = {
  "v2/app": CoreAppState,
  "v2/modpacks": ModpackState,
  "v2/instances": InstanceState,
  "v2/install": InstallState,
  "v2/news": NewsState,
  "v2/dialogs": DialogsState,
  "v2/ads": AdsState,
  "v2/mtauth": MTAuthState
  "v2/apiCredentials": ApiCredentialsState,
  "v2/userFavourites": UserFavouritesState,
}

export type AppStoreModules = keyof AppState;

export function ns(module: AppStoreModules) {
  return { namespace: module }
}

export function nsAction(module: AppStoreModules, action: string) {
  return `${module}/${action}`
}
