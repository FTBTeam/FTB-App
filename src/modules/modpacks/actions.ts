import {ActionTree} from 'vuex';
import {ModpackState, ModPack, Instance, InstallProgress, Changelog} from './types';
import config from '@/config';
import {RootState} from '@/types';
import {asyncForEach, logVerbose} from '@/utils';
import semver from 'semver';
import { AuthState } from '../auth/types';

export function getAPIRequest(rootState: RootState, url: string): Promise<Response> {
    if (rootState.auth === null) {
        return fetch(`${config.apiURL}/public/${url}`);
    }
    const auth: AuthState = rootState.auth as AuthState;
    if (auth.token === null || auth.token.attributes.modpackschkey === undefined) {
        return fetch(`${config.apiURL}/public/${url}`);
    }
    return fetch(`${config.apiURL}/${auth.token.attributes.modpackschkey}/${url}`, {headers: {
        Secret: auth.token.attributes.modpackssecret,
    }});
}

export const actions: ActionTree<ModpackState, RootState> = {
    doSearch({commit, rootState, dispatch}: any, searchTerm): any {
        if (searchTerm.trim().length < 3) {
            return;
        }
        commit('setLoading', true);
        commit('setSearch', searchTerm);
        return getAPIRequest(rootState, `modpack/search/8?term=${searchTerm}`)
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
                const pack = await dispatch('fetchModpack', packID);
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
        return fetch(`${config.apiURL}/public/modpack/popular/installs/10`)
        .then((response) => response.json())
        .then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                const pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
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
    getPopularPlays({commit, rootState, dispatch}: any): any {
        commit('setLoading', true);
        return fetch(`${config.apiURL}/public/modpack/popular/plays/10`)
        .then((response) => response.json())
        .then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                const pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
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
    getPrivatePacks({commit, rootState, dispatch}: any): any {
        commit('setLoading', true);
        return getAPIRequest(rootState, `modpack/private/10`)
        .then((response) => response.json())
        .then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                const pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
                    return;
                }
                packs.push(pack);
            });
            commit('privatePacks', packs);
            commit('setLoading', false);
        }).catch((err) => {
            commit('privatePacksError', err);
            commit('setLoading', false);
            console.error(err);
        });
    },
    clearSearch({commit}): any {
        const packs: ModPack[] = [];
        commit('searchLoaded', packs);
        commit('setSearch', '');
        commit('setLoading', false);
    },
    loadFeaturedPacks({commit, rootState, dispatch}: any): any {
        commit('setLoading', true);
        return fetch(`${config.apiURL}/public/modpack/featured/10`)
        .then((response) => response.json()).then(async (data) => {
            const packIDs = data.packs;
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                const pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
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
    loadAllPacks({commit, rootState, dispatch}: any): any {
        commit('setLoading', true);
        console.log("Loading all packs...");
        return fetch(`${config.apiURL}/public/modpack/all`)
        .then((response) => response.json()).then(async (data) => {
            const packIDs = data.packs;
            console.log("Loaded packs");
            console.log(packIDs);
            if (packIDs == null) {
                return;
            }
            const packs: ModPack[] = [];
            await asyncForEach(packIDs, async (packID: number) => {
                const pack = await dispatch('fetchModpack', packID);
                if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                    logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
                    return;
                }
                packs.push(pack);
            });
            commit('allPacksLoaded', packs);
            commit('setLoading', false);
        }).catch((err) => {
            commit('allPacksError', err);
            commit('setLoading', false);
            console.error(err);
        });
    },
    storeInstalledPacks({commit}, packsPayload): any {
        const packs: Instance[] = [];
        asyncForEach(Object.keys(packsPayload.instances), async (index) => {
            const instance: Instance = packsPayload.instances[index];
            instance.kind = 'instance';
            packs.push(instance);
        });
        asyncForEach(Object.keys(packsPayload.cloudInstances), async (index) => {
            const instance: Instance = packsPayload.cloudInstances[index];
            instance.kind = 'cloudInstance';
            packs.push(instance);
        });
        commit('storeInstalledPacks', packs);
    },
    async updateInstall({commit, rootState}: any, install: InstallProgress): Promise<any> {
        if (rootState.modpacks.packsCache[install.modpackID]) {
            install.pack = rootState.modpacks.packsCache[install.modpackID];
        } else {
            const pack = await fetch(`${config.apiURL}/public/modpack/${install.modpackID}`)
            .then((response) => response.json());
            install.pack = pack;
            logVerbose(rootState, 'Adding to cache', pack);
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
    refreshPacks({dispatch, rootState}): Promise<any> {
        return new Promise((resolve, reject) => {
            dispatch('sendMessage', {
                payload: { type: 'installedInstances' },
                callback: (data: any) => {
                    logVerbose(rootState, 'Storing installed packs', data);
                    dispatch('storeInstalledPacks', data);
                    resolve(data.instances);
            }}, {root: true});
        });
    },
    saveInstance({dispatch, commit}, instance: Instance): Promise<any> {
        return new Promise((resolve, reject) => {
            dispatch('sendMessage', {payload: {type: 'instanceConfigure', uuid: instance.uuid, instanceInfo: {name: instance.name, jvmargs: instance.jvmArgs, memory: instance.memory, width: instance.width, height: instance.height, cloudSaves: instance.cloudSaves, jrePath: instance.jrePath, embeddedjre: instance.jrePath === ""}}, callback: (msg: any) => {
                dispatch('sendMessage', {
                    payload: { type: 'installedInstances', refresh: true},
                    callback: (data: any) => {
                      dispatch('storeInstalledPacks', data);
                      resolve();
                }}, {root: true});
            }}, {root: true});
        });
    },
    getChangelog({commit, rootState}, {packID, versionID}): Promise<any> {
        return new Promise(async (resolve, reject) => {
            const changelog = await getAPIRequest(rootState, `modpack/${packID}/${versionID}/changelog`).catch((err) =>  console.error(err))
            .then((response: any) => {
                response = response as Response;
                return response.json();
            }) as Changelog;
            resolve(changelog);
        });
    },
    fetchModpack({commit, rootState}: any, packID): Promise<any> {
        logVerbose(rootState, 'Fetching modpack', packID);
        if (rootState.modpacks.packsCache[packID]) {
            logVerbose(rootState, 'Found in cache', packID);
            return new Promise((resolve, reject) => resolve(rootState.modpacks.packsCache[packID]));
        }
        return new Promise(async (resolve, reject) => {
            logVerbose(rootState, 'Fetching...', packID);
            await getAPIRequest(rootState, `modpack/${packID}`).catch((err) =>  console.error(err))
            .then(async (response: any) => {
                response = response as Response;
                const pack: ModPack = await response.json() as ModPack;
                if (pack === undefined) {
                    reject("Pack is unavailable");
                    return;
                }

                if (pack.versions !== undefined) {

                    pack.versions.forEach((version) => {
                        version.mtgID = btoa(pack.id + '' + version.id);
                    });

                    pack.versions = pack.versions.sort((a, b) => {
                        return semver.rcompare(a.name, b.name);
                    });
                }
                pack.kind = 'modpack';
                logVerbose(rootState, 'Adding to cache', pack);
                commit('addToCache', pack);
                logVerbose(rootState, 'Resolving...', packID);
                resolve(pack);
            }).catch((err) => {
                console.error('Error getting modpack', err);
                reject("Pack is unavailable");
            });
        });
    },
    async refreshCache({commit, rootState, dispatch}: any): Promise<any> {
        const packIDs = Object.keys(rootState.modpacks.packsCache);
        commit('clearCache');
        await Promise.all(packIDs.map(async (id: any) => {
            return await dispatch('fetchModpack', id);
        }));
        await dispatch('loadFeaturedPacks');
        await dispatch('loadAllPacks');
        await dispatch('getPopularPlays');
        await dispatch('getPopularInstalls');
    },
};
