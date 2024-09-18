export enum CoreMutations {
  ADD_PROFILE = 'addProfile',
  LOAD_PROFILES = 'loadProfiles',
  SET_ACTIVE_PROFILE = 'setActiveProfile',
  OPEN_SIGNIN = 'openSignIn',
  REMOVE_PROFILE = 'removeProfile',
  INSTANCE_LOADING = "instanceLoading",
  TOGGLE_DEBUG_AD_ASIDE = "toggleDebugDisableAdAside"
}

export interface AuthProfile {
  username: string;
  uuid: string;
}

export type CoreState = {
  profiles: AuthProfile[];
  activeProfile?: AuthProfile;
  signInOpened: boolean;
  instanceLoading: boolean;
  debugDisableAdAside: boolean;
};
