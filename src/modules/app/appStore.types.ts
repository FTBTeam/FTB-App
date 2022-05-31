export enum AppStoreMutations {
  SET_RUNNING_PACK = 'setRunningPack',
  INSTALL_PACK = 'installPack',
}

export type InstallingState = {
  meta: {
    art?: string;
    name: string;
    version: string;
  };
};

export type AppStoreState = {
  pack: {
    currentlyRunning: string | null;
  };
  installing: InstallingState | null;
};
