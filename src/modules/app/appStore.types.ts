export enum AppStoreMutations {
  SET_RUNNING_PACK = 'setRunningPack',
}

export type AppStoreState = {
  pack: {
    currentlyRunning: string | null;
  };
};
