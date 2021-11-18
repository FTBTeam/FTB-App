export enum CoreMutations {
  ADD_PROFILE = 'addProfile',
}

export interface AuthProfile {
  isAuthenticated: boolean;
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
