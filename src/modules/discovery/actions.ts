import { ModpackState, ModPack, ModPackTag } from './../modpacks/types';
import {ActionTree} from 'vuex';
import config from '@/config';
import { DiscoveryState } from './types';
import {RootState} from '@/types';
import Parser from 'rss-parser';

function inCommon(a: any[], b: any[]){
    let count = 0;
    for (var i = 0; i < a.length; i++){
        if(b.indexOf(a[i]) > -1){
            count++;
        }
    }
    return count;
}

export const actions: ActionTree<DiscoveryState, RootState> = {
    async fetchQueue({rootState, commit}): Promise<any> {
        let installedPacks: ModPack[] = await this.dispatch('modpacks/refreshPacks', {}, {root: true});
        console.log("Fetching queue", installedPacks);
        commit('setLoading', true);
        if(installedPacks.length > 0) {
            console.log("Getting discovery based on installed modpacks");
            // Get all installed pack IDS
            let packIDs: number[] = installedPacks.map((pack) => {
                return pack.id;
            });
            console.log("Pack IDs is", packIDs);
            
            let allTags: ModPackTag[] = [];
            // Get list of tags for all the installed packs
            let tags: ModPackTag[][] = await Promise.all(packIDs.map(async (packID) => {
                let pack: ModPack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                console.log("pack", pack);
                return pack.tags;
            }));
            // Merge tag lists into one list
            tags.forEach((tagList) => {
                console.log("Tag list", tagList);
                allTags = allTags.concat(tagList)
            });
            console.log("All tags", allTags);
            // for each tag get top 3 most popular modpack ID's
            let foundPackIDs = await Promise.all(allTags.map(async (tag: ModPackTag) => {
                console.log(tag);
                return await fetch(`${config.apiURL}/public/modpack/popular/installs/${tag.name}/3`).catch((err) =>  console.error(err))
                .then(async (response: any) => {
                    response = response as Response;
                    let packs = (await response.json()).packs;
                    return packs.slice(0, 3);
                }).catch((err) => {
                    console.error('Error getting modpacks', err);
                });
            }));
            let allPacks: number[] = [];
            foundPackIDs.forEach((packList) => {
                allPacks = allPacks.concat(packList)
            });
            console.log("Found pack ids", allPacks);
            // for each modpack id get modpack
            let packs: ModPack[] = await Promise.all(allPacks.map(async (packID) => {
                let pack: ModPack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                return pack;
            }));

            console.log("Found packs", packs);
            packs = packs.filter((pack) => {
                if(packIDs.indexOf(pack.id) !== -1){
                    return false;
                }
                return true;
            })

            // Sort by most amount of tags incommon            
            packs = packs.sort((a, b) => {
                return inCommon(a.tags, allTags) - inCommon(b.tags, allTags);
            })
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
                const packs: ModPack[] = await Promise.all(packIDs.map(async (packID: number) => {
                    const pack = await this.dispatch('modpacks/fetchModpack', packID, {root: true});
                    if (pack.status !== undefined && pack.status === 'error' || pack.versions.length <= 0) {
                        console.log(`ERR: Modpack ID ${packID} has no versions`);
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
        }
    },
};
