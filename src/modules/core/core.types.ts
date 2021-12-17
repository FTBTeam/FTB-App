export enum CoreMutations {
  ADD_PROFILE = 'addProfile',
  LOAD_PROFILES = 'loadProfiles',
  SET_ACTIVE_PROFILE = 'setActiveProfile',
  OPEN_SIGNIN = 'openSignIn',
  REMOVE_PROFILE = 'removeProfile',
  INSTANCE_LOADING = "instanceLoading"
}

export interface AuthProfile {
  type: 'mojang' | 'microsoft';
  token: string;
  refreshToken: string;
  tokens:
    | {
        accessToken: string;
        refreshToken: string;
      }
    | {
        accessToken: string;
        clientToken: string;
      };
  username: string;
  uuid: string;
  expiresAt?: number;
}

export type CoreState = {
  profiles: AuthProfile[];
  activeProfile?: AuthProfile;
  signInOpened: {open: boolean, jumpToAuth?: 'ms' | 'mc' | null, uuid: string | null};
  instanceLoading: boolean;
};
