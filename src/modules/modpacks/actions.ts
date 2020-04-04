import {ActionTree} from 'vuex';
import {ModpackState, ModPack, Instance, InstallProgress, Changelog} from './types';
import config from '@/config';
import {RootState} from '@/types';
import {asyncForEach} from '@/utils';
import semver from 'semver';

export const actions: ActionTree<ModpackState, RootState> = {
    doSearch({commit, rootState, dispatch}: any, searchTerm): any {
        if (searchTerm.trim().length <= 3) {
            return;
        }
        commit('setLoading', true);
        fetch(`${config.apiURL}/public/modpack/search/8?term=${searchTerm}`)
        .then((response) => response.json())
        .then(async (data) => {
            if (data.status === 'error') {
                commit('searchError', data.message);
                commit('setLoading', false);
                return;
            }
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                let pack = await dispatch('fetchModpack', packID);
                packs.push(pack);
            });
            commit('searchLoaded', packs);
            commit('setLoading', false);
        }).catch((err) => {
            commit('searchError', err);
            commit('setLoading', false);
            console.error(err);
        });
    },
    getPopularInstalls({commit, rootState, dispatch}: any): any {
        commit('setLoading', true);
        fetch(`${config.apiURL}/public/modpack/popular/installs/10`)
        .then((response) => response.json())
        .then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                let pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    console.log(`ERR: Modpack ID ${packID} has no versions`);
                    return;
                }
                packs.push(pack);
            });
            commit('popularInstalls', packs);
            commit('setLoading', false);
        }).catch((err) => {
            commit('popularInstallsError', err);
            commit('setLoading', false);
            console.error(err);
        });
    },
    getPopularPlays({commit, rootState, dispatch}:any): any {
        commit('setLoading', true);
        fetch(`${config.apiURL}/public/modpack/popular/plays/10`)
        .then((response) => response.json())
        .then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                let pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    console.log(`ERR: Modpack ID ${packID} has no versions`);
                    return;
                }
                packs.push(pack);
            });
            commit('popularPlays', packs);
            commit('setLoading', false);
        }).catch((err) => {
            commit('popularPlaysError', err);
            commit('setLoading', false);
            console.error(err);
        });
    },
    clearSearch({commit}): any {
        const packs: ModPack[] = [];
        commit('searchLoaded', packs);
        commit('setLoading', false);
    },
    loadFeaturedPacks({commit, rootState, dispatch}: any): any {
        commit('setLoading', true);
        fetch(`${config.apiURL}/public/modpack/featured/5`)
        .then((response) => response.json()).then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                let pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    console.log(`ERR: Modpack ID ${packID} has no versions`);
                    return;
                }
                packs.push(pack);
            });
            commit('featuredPacksLoaded', packs);
            commit('setLoading', false);
        }).catch((err) => {
            commit('featuredPacksError', err);
            commit('setLoading', false);
            console.error(err);
        });
    },
    storeInstalledPacks({commit}, packsPayload): any {
        const packs: Instance[] = [];

        asyncForEach(Object.keys(packsPayload.instances), async (index) => {
            const instance: Instance = packsPayload.instances[index];
            packs.push(instance);
        });
        commit('storeInstalledPacks', packs);
    },
    async updateInstall({commit, rootState}: any, install: InstallProgress): Promise<any> {
        if(rootState.modpacks.packsCache[install.modpackID]){
            install.pack = rootState.modpacks.packsCache[install.modpackID];
        } else {
            let pack = await fetch(`${config.apiURL}/public/modpack/${install.modpackID}`)
            .then((response) => response.json());
            install.pack = pack;
            commit('addToCache', pack);
        }
        commit('updateInstall', install);
    },
    finishInstall({commit}, install: InstallProgress): any {
        commit('finishInstall', install);
    },
    errorInstall({commit}, install: InstallProgress): any {
        commit('errorInstall', install);
    },
    refreshPacks({dispatch}): any {
        return new Promise((resolve, reject) => {
            dispatch('sendMessage', {
                payload: { type: 'installedInstances' },
                callback: (data: any) => {
                dispatch('storeInstalledPacks', data);
                resolve();
            }}, {root: true});
        });
    },
    saveInstance({dispatch, commit}, instance: Instance): Promise<any> {
        return new Promise((resolve, reject) => {
            dispatch('sendMessage', {payload: {type: 'instanceConfigure', uuid: instance.uuid, instanceInfo: {name: instance.name, jvmargs: instance.jvmArgs, memory: instance.memory, width: instance.width, height: instance.height}}, callback: (msg: any) => {
                dispatch('sendMessage', {
                    payload: { type: 'installedInstances' },
                    callback: (data: any) => {
                      dispatch('storeInstalledPacks', data);
                      resolve();
                }}, {root: true});
            }}, {root: true});
        });
    },
    getChangelog({commit}, {packID, versionID}): Promise<any> {
        return new Promise(async (resolve, reject) => {
            const changelog = await fetch(`${config.apiURL}/public/modpack/${packID}/${versionID}/changelog`).catch((err) =>  console.error(err))
            .then((response: any) => {
                response = response as Response;
                return response.json();
            }) as Changelog;
            resolve(changelog);
        });
    },
    fetchModpack({commit, rootState}: any, packID): Promise<any> {
        if(rootState.modpacks.packsCache[packID]){
            return rootState.modpacks.packsCache[packID];
        }
        return new Promise(async (resolve, reject) => {
            await fetch(`${config.apiURL}/public/modpack/${packID}`).catch((err) =>  console.error(err))
            .then(async (response: any) => {
                response = response as Response;
                let pack: ModPack = await response.json() as ModPack;
                pack.versions = pack.versions.sort((a, b) => {
                    return semver.rcompare(a.name, b.name);
                });
                commit('addToCache', pack);
                resolve(pack);
            }).catch((err) => {
                console.error("Error getting modpack", err);
                reject(err);
            });
        });
    },
};
