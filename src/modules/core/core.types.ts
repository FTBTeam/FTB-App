export enum CoreMutations {
  ADD_PROFILE = 'addProfile',
  LOAD_PROFILES = 'loadProfiles',
  SET_ACTIVE_PROFILE = 'setActiveProfile',
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
}

export type CoreState = {
  profiles: AuthProfile[];
  activeProfile?: AuthProfile;
};
