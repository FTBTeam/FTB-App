import { state } from './index';
import {MutationTree} from 'vuex';
import { ModpackState, ModPack, Instance, InstallProgress } from './types';

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
    updateInstall(state, payload: InstallProgress) {
        if (state.installing !== null) {
            const foundPack = state.installing
            foundPack.progress = payload.progress;
            foundPack.error = payload.error || false;
            foundPack.errorMessage = payload.errorMessage || '';
            foundPack.downloadSpeed = payload.downloadSpeed || 0;
            foundPack.stage = payload.stage || 'INIT';
            foundPack.downloadedBytes = payload.downloadedBytes || 0;  
            foundPack.totalBytes = payload.totalBytes || 0;
            foundPack.instanceID = payload.instanceID;
            if(payload.message){
                foundPack.message = payload.message;
            }
            if(payload.files){
                foundPack.files = payload.files
            } else  if(foundPack.files === undefined){
                foundPack.files = [];
            }
            state.installing = foundPack;
        } else {
            if(payload.files === undefined) {
                payload.files = [];
            }
            state.installing = payload;
        }
    },
    finishInstall(state, payload: InstallProgress) {
        state.installing = null;
    },
    errorInstall(state, payload: InstallProgress) {

    },
    setLoading(state, isLoading: boolean) {
        state.loading = isLoading;
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
    setLaunchProgress(state, progress: any){
        state.launchProgress = progress;
    }
};
