import { MutationTree } from 'vuex';
import { InstallProgress, Instance, ModPack, ModpackState } from './types';

export const mutations: MutationTree<ModpackState> = {
  searchLoaded(state, payload: ModPack[]) {
    state.error = false;
    state.search = payload;
  },
  searchError(state, payload: string) {
    state.error = true;
    state.errorMsg = payload;
    state.search = [];
  },
  searchCurseLoaded(state, payload: ModPack[]) {
    state.error = false;
    state.searchCurse = payload;
  },
  searchCurseError(state, payload: string) {
    state.error = true;
    state.errorMsg = payload;
    state.searchCurse = [];
  },
  popularInstalls(state, payload: ModPack[]) {
    state.error = false;
    state.popularInstalls = payload;
  },
  popularInstallsError(state) {
    state.error = true;
    state.popularInstalls = [];
  },
  popularPlays(state, payload: ModPack[]) {
    state.error = false;
    state.popularPlays = payload;
  },
  popularPlaysError(state) {
    state.error = true;
    state.popularPlays = [];
  },
  privatePacks(state, payload: ModPack[]) {
    state.error = false;
    state.privatePacks = payload;
  },
  privatePacksError(state) {
    state.error = true;
    state.privatePacks = [];
  },
  featuredPacksLoaded(state, payload: ModPack[]) {
    state.error = false;
    state.featuredPacks = payload;
  },
  allPacksLoaded(state, payload: ModPack[]) {
    state.error = false;
    state.all = payload;
  },
  featuredPacksError(state) {
    state.error = true;
    state.featuredPacks = [];
  },
  storeInstalledPacks(state, payload: Instance[]) {
    state.installedPacks = payload;
  },
  pushToInstalledPack(state, payload: { pack: Instance; existing: boolean }) {
    // Prevent pack duplication if somehow the pack is installed / updated at the sametime...
    // Mostly happens on users clicking buttons to fast and the subprocess not cancelling the installation quick enough
    if (payload.existing) {
      state.installedPacks = state.installedPacks.filter((e) => e.uuid !== payload.pack.uuid);
    }

    state.installedPacks.push(payload.pack);
  },
  updatePackInStore(state, payload: Instance) {
    state.installedPacks = state.installedPacks.map((e) => (e.uuid === payload.uuid ? payload : e));
  },
  updateInstall(state, payload: InstallProgress) {
    if (state.installing !== null) {
      const foundPack = state.installing;
      foundPack.progress = payload.progress;
      foundPack.error = payload.error || false;
      foundPack.errorMessage = payload.errorMessage || '';
      foundPack.downloadSpeed = payload.downloadSpeed || 0;
      foundPack.stage = payload.stage || 'INIT';
      foundPack.downloadedBytes = payload.downloadedBytes || 0;
      foundPack.totalBytes = payload.totalBytes || 0;
      foundPack.instanceID = payload.instanceID;
      if (payload.message) {
        foundPack.message = payload.message;
      }
      if (payload.files) {
        foundPack.files = payload.files;
      } else if (foundPack.files === undefined) {
        foundPack.files = [];
      }
      state.installing = foundPack;
    } else {
      if (payload.files === undefined) {
        payload.files = [];
      }
      state.installing = payload;
    }
  },
  finishInstall(state, payload: InstallProgress) {
    state.installing = null;
  },
  errorInstall(state, payload: InstallProgress) {},
  setLoading(state, isLoading: boolean) {
    state.loading = isLoading;
  },
  // This needs a completely re-write at some point to avoid having two loading states
  curseLoading(state, payload) {
    state.curseLoading = payload;
  },
  storeModpack(state, modpack: ModPack) {
    state.currentModpack = modpack;
  },
  addToCache(state, modpack: ModPack) {
    if (modpack === undefined) {
      return;
    }
    if (!state.packsCache[modpack.id]) {
      state.packsCache[modpack.id] = modpack;
    }
  },
  clearCache(state) {
    state.packsCache = [];
  },
  setSearch(state, search: string) {
    state.searchString = search;
  },
  clearSearch(state) {
    state.searchString = '';
  },
};
