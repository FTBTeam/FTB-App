export enum CoreMutations {
  ADD_PROFILE = 'addProfile',
  LOAD_PROFILES = 'loadProfiles',
}

export interface AuthProfile {
  type: 'mojang' | 'microsoft';
  token: string;
  username: string;
  name: string;
  id: string;
}

export type CoreState = {
  profiles: AuthProfile[];
  activeProfile?: string;
};
