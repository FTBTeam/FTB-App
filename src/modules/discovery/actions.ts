import { ModpackState, ModPack, ModPackTag } from './../modpacks/types';
import {ActionTree} from 'vuex';
import config from '@/config';
import {logVerbose} from '@/utils';
import { DiscoveryState } from './types';
import {RootState} from '@/types';
import Parser from 'rss-parser';

function inCommon(a: any[], b: any[]) {
    let count = 0;
    for (let i = 0; i < a.length; i++) {
        if (b.indexOf(a[i]) > -1) {
            count++;
        }
    }
    return count;
}



export const actions: ActionTree<DiscoveryState, RootState> = {
    async fetchQueue({rootState, commit}): Promise<any> {
        const installedPacks: ModPack[] = await this.dispatch('modpacks/refreshPacks', {}, {root: true});
        logVerbose(rootState, 'Fetching queue', installedPacks);
        commit('setLoading', true);
        if (installedPacks.length > 0) {
            logVerbose(rootState, 'Getting discovery based on installed modpacks');
            // Get all installed pack IDS
            const packIDs: number[] = installedPacks.map((pack) => {
                return pack.id;
            });
            logVerbose(rootState, 'Pack IDs is', packIDs);

            let allTags: ModPackTag[] = [];
            // Get list of tags for all the installed packs
            const tags: ModPackTag[][] = await Promise.all(packIDs.map(async (packID) => {
                const pack: ModPack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                logVerbose(rootState, 'pack', pack);
                return pack.tags;
            }));
            // Merge tag lists into one list
            tags.forEach((tagList) => {
                logVerbose(rootState, 'Tag list', tagList);
                allTags = allTags.concat(tagList);
            });
            allTags = allTags.filter((tag) => tag !== undefined);
            if (allTags.length === 0 ) {
                fetch(`${config.apiURL}/public/modpack/popular/installs/10`)
                .then((response) => response.json())
                .then(async (data) => {
                    const packIDs = data.packs;
                    if (packIDs == null) {
                        return;
                    }
                    const packs: ModPack[] = await Promise.all(packIDs.map(async (packID: number) => {
                        const pack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                        if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                            logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
                            return;
                        }
                        return pack;
                    }));
                    commit('loadQueue', packs);
                    commit('setLoading', false);
                }).catch((err) => {
                    commit('setLoading', false);
                    console.error(err);
                });
                return;
            }
            logVerbose(rootState, 'All tags', allTags);
            // for each tag get top 3 most popular modpack ID's
            const foundPackIDs = await Promise.all(allTags.map(async (tag: ModPackTag) => {
                logVerbose(rootState, tag);
                return await fetch(`${config.apiURL}/public/modpack/popular/installs/${tag.name}/50`).catch((err) =>  console.error(err))
                .then(async (response: any) => {
                    response = response as Response;
                    const packs = (await response.json()).packs;
                    return packs;
                }).catch((err) => {
                    console.error('Error getting modpacks', err);
                });
            }));
            const allPacks: number[] = [];
            foundPackIDs.forEach((packList) => {
                packList.forEach((p: number) => {
                    if (allPacks.indexOf(p) === -1) {
                        allPacks.push(p);
                    }
                });
            });
            logVerbose(rootState, 'Found pack ids', allPacks);
            // for each modpack id get modpack
            let packs: ModPack[] = await Promise.all(allPacks.map(async (packID) => {
                const pack: ModPack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                return pack;
            }));

            logVerbose(rootState, 'Found packs', packs);
            packs = packs.filter((pack) => {
                if (packIDs.indexOf(pack.id) !== -1) {
                    return false;
                }
                return true;
            });

            packs = packs.filter((pack) => {
                if (pack.links.length < 1) {
                    return false;
                }
                if (pack.links.filter((l) => l.type === 'video').length < 1) {
                    return false;
                }
                return true;
            });

            // Sort by most amount of tags incommon
            packs = packs.sort((a, b) => {
                return inCommon(a.tags, allTags) - inCommon(b.tags, allTags);
            });
            packs = packs.slice(0, packs.length > 10 ? 9 : packs.length - 1);
            commit('loadQueue', packs);

            // Sort by most amount of installs

        } else {
            fetch(`${config.apiURL}/public/modpack/popular/installs/10`)
            .then((response) => response.json())
            .then(async (data) => {
                const packIDs = data.packs;
                if (packIDs == null) {
                    return;
                }
                let packs: ModPack[] = await Promise.all(packIDs.map(async (packID: number) => {
                    const pack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                    if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                        logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
                        return;
                    }
                    return pack;
                }));
                packs = packs.filter((pack) => {
                    if (pack.links.length < 1) {
                        return false;
                    }
                    if (pack.links.filter((l) => l.type === 'video').length < 1) {
                        return false;
                    }
                    return true;
                });
                commit('loadQueue', packs);
                commit('setLoading', false);
            }).catch((err) => {
                commit('setLoading', false);
                console.error(err);
            });
        }
    },
};
