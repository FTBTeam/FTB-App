export enum AppStoreMutations {
  SET_RUNNING_PACK = 'setRunningPack',
  INSTALL_PACK = 'installPack',
}

export type InstallerState = {
  pack: InstallerRequest;
  meta: {
    art?: string;
    name: string;
    version: string;
    isUpdate?: boolean;
  };
};

export type InstallerRequest = {
  uuid?: string;
  id?: string | number;
  version?: string | number;
  shareCode?: string;
  private?: boolean;
  importFrom?: string;
  name?: string;
  packType?: string | number; // 0 = modpackch | 1 = curseforge
};

export type AppStoreState = {
  pack: {
    currentlyRunning: string | null;
  };
  installer: InstallerState | null;
};
