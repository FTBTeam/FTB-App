import { ActionTree } from 'vuex';
import { Changelog, InstallProgress, Instance, ModPack, ModpackState } from './types';
import { RootState } from '@/types';
import {asyncForEach, getLogger, Logger, logVerbose} from '@/utils';
import semver from 'semver';
import {AuthState} from '@/modules/auth/types';

const packIdBlacklist = [
  104,
  105,
  116
];

/**
 * @deprecated DO NOT USE!
 */
export function getAPIRequest(rootState: RootState, url: string): Promise<Response> {
  if (rootState.auth === null) {
    return fetch(`${process.env.VUE_APP_MODPACK_API}/public/${url}`);
  }
  const auth: AuthState = rootState.auth as AuthState;
  if (auth.token === null || auth.token.attributes.modpackschkey === undefined) {
    return fetch(`${process.env.VUE_APP_MODPACK_API}/public/${url}`);
  }
  return fetch(`${process.env.VUE_APP_MODPACK_API}/${auth.token.attributes.modpackschkey}/${url}`, {
    headers: {
      Secret: auth.token.attributes.modpackssecret,
    },
  });
}

export const actions: ActionTree<ModpackState, RootState> = {
  // async loadAllPacks({ commit, rootState, dispatch, state }): Promise<any> {
  //   if (state.isPullingPacks) {
  //     return;
  //   }
  //  
  //   // Eww
  //   const cleanUp = (loading: boolean = false) => {
  //     commit('setLoading', loading);
  //     commit('setPacksToLoad', 0)
  //     commit('setPacksLoaded', 0)
  //     commit('setIsPullingPacks', loading);
  //   }
  //  
  //   logger().info('Loading all modpacks from the api');
  //   cleanUp(true);
  //  
  //   try {
  //     const allPacks = await fetch(`${process.env.VUE_APP_MODPACK_API}/public/modpack/all`);
  //     let {packs}: {packs: number[]} = await allPacks.json();
  //    
  //     if (packs == null || packs.length <= 0) {
  //       cleanUp();
  //       return;
  //     }
  //    
  //     packs = packs.filter(packId => packIdBlacklist.indexOf(packId) === -1);
  //     commit('setPacksToLoad', packs.length);
  //     logger().info(`Received ${packs.length} from the api`);
  //
  //     const packChunks = chunkArray(packs,5);
  //     const loadedPackChunks = await Promise.all(packChunks.map(async (chunk) => {
  //       let loadedPacksHolder = [];
  //       for (const packID of chunk) {
  //         logger().info(`Loading ${packID}`);
  //        
  //         const loadedPack = await dispatch('fetchModpack', packID);
  //         if (!loadedPack || (loadedPack.status !== undefined && loadedPack.status === 'error') || loadedPack.versions.length <= 0) {
  //           logVerbose(rootState, `ERR: Modpack ID ${packID} has no versions`);
  //           continue;
  //         }
  //        
  //         loadedPacksHolder.push(loadedPack)
  //         commit('updatePacksLoaded')
  //       }
  //      
  //       return loadedPacksHolder as ModPack[];
  //     }))
  //    
  //     const loadedPacks = loadedPackChunks.flat();
  //     const sortedPacks = loadedPacks.sort((a, b) => {
  //       if (a.featured !== null && a.featured) {
  //         if (b.featured !== null && b.featured) {
  //           return 0;
  //         } else {
  //           return -1;
  //         }
  //       } else {
  //         return 0;
  //       }
  //     });
  //
  //     commit('allPacksLoaded', sortedPacks);
  //     commit('setLoading', false);
  //     commit('setIsPullingPacks', false);
  //   } catch (error) {
  //     cleanUp()
  //     commit('allPacksError', error);
  //     commit('setLoading', false);
  //   } finally {
  //     cleanUp()
  //   }
  // },
  storeInstalledPacks({ commit }, packsPayload): any {
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
  updatePackInStore({ commit }, payload: Instance): any {
    commit('updatePackInStore', payload);
  },
  async updateInstall({ commit, rootState }: any, install: InstallProgress): Promise<any> {
    if (rootState.modpacks.packsCache[install.modpackID]) {
      install.pack = rootState.modpacks.packsCache[install.modpackID];
    } else {
      const pack = await fetch(`${process.env.VUE_APP_MODPACK_API}/public/modpack/${install.modpackID}`).then(
        (response) => response.json(),
      );
      install.pack = pack;
      logVerbose(rootState, 'Adding to cache', pack);
      commit('addToCache', pack);
    }
    commit('updateInstall', install);
  },
  finishInstall({ commit }, install: InstallProgress): any {
    commit('finishInstall', install);
  },
  errorInstall({ commit }, install: InstallProgress): any {
    commit('errorInstall', install);
  },
  refreshPacks({ dispatch, rootState }): Promise<any> {
    return new Promise((resolve, reject) => {
      dispatch(
        'sendMessage',
        {
          payload: { type: 'installedInstances' },
          callback: (data: any) => {
            logVerbose(rootState, 'Storing installed packs', data);
            dispatch('storeInstalledPacks', data);
            resolve(data.instances);
          },
        },
        { root: true },
      );
    });
  },
  saveInstance({ dispatch, commit }, instance: Instance): Promise<any> {
    return new Promise((resolve, reject) => {
      dispatch(
        'sendMessage',
        {
          payload: {
            type: 'instanceConfigure',
            uuid: instance.uuid,
            instanceInfo: {
              name: instance.name,
              jvmargs: instance.jvmArgs,
              jrePath: instance.jrePath,
              embeddedJre: instance.jrePath === '',
              memory: instance.memory,
              width: instance.width,
              height: instance.height,
              cloudSaves: instance.cloudSaves,
            },
          },
          callback: async (msg: any) => {
            await new Promise((resolve2) =>
              dispatch(
                'sendMessage',
                {
                  payload: { type: 'installedInstances', refresh: true },
                  callback: (data: any) => {
                    dispatch('storeInstalledPacks', data);
                    resolve2(null);
                  },
                },
                { root: true },
              ),
            );

            resolve(msg);
          },
        },
        { root: true },
      );
    });
  },
  getChangelog({ commit, rootState }, { packID, versionID, type = 0 }): Promise<any> {
    return new Promise(async (resolve, reject) => {
      let modpackRoute = type == 0 ? 'modpack' : 'curseforge';
      const changelog = (await getAPIRequest(rootState, `${modpackRoute}/${packID}/${versionID}/changelog`)
        .catch((err) => console.error(err))
        .then((response: any) => {
          response = response as Response;
          return response.json();
        })) as Changelog;
      resolve(changelog);
    });
  },
  fetchModpack({ commit, rootState }: any, packID): Promise<any> {
    logVerbose(rootState, 'Fetching modpack', packID);
    if (rootState.modpacks.packsCache[packID]) {
      logVerbose(rootState, 'Found in cache', packID);
      return new Promise((resolve, reject) => resolve(rootState.modpacks.packsCache[packID]));
    }
    return new Promise(async (resolve, reject) => {
      logVerbose(rootState, 'Fetching...', packID);
      await getAPIRequest(rootState, `modpack/${packID}`)
        .catch((err) => console.error(err))
        .then(async (response: any) => {
          response = response as Response;
          const pack: ModPack = (await response.json()) as ModPack;
          if (pack === undefined) {
            reject('Pack is unavailable');
            return;
          }

          if (pack.versions !== undefined) {
            pack.versions.forEach((version) => {
              version.mtgID = btoa(pack.id + '' + version.id);
            });
            try {
              pack.versions = pack.versions.sort((a, b) => {
                return semver.rcompare(a.name, b.name);
              });
            } catch (e) {
              pack.versions = pack.versions.reverse();
            }
          }
          pack.kind = 'modpack';
          logVerbose(rootState, 'Adding to cache', pack);
          commit('addToCache', pack);
          logVerbose(rootState, 'Resolving...', packID);
          resolve(pack);
        })
        .catch((err) => {
          console.error('Error getting modpack', err);
          reject('Pack is unavailable');
        });
    });
  },
  fetchCursepack({ commit, rootState }: any, packID): Promise<any> {
    logVerbose(rootState, 'Fetching modpack', packID);
    if (rootState.modpacks.packsCache[packID]) {
      logVerbose(rootState, 'Found in cache', packID);
      return new Promise((resolve, reject) => resolve(rootState.modpacks.packsCache[packID]));
    }
    return new Promise(async (resolve, reject) => {
      logVerbose(rootState, 'Fetching...', packID);
      await getAPIRequest(rootState, `curseforge/${packID}`)
        .catch((err) => console.error(err))
        .then(async (response: any) => {
          response = response as Response;
          const pack: ModPack = (await response.json()) as ModPack;
          if (pack === undefined) {
            reject('Pack is unavailable');
            return;
          }

          if (pack.versions !== undefined) {
            pack.versions.forEach((version) => {
              version.mtgID = btoa(pack.id + '' + version.id);
            });
            try {
              pack.versions = pack.versions.sort((a, b) => {
                return semver.rcompare(a.name, b.name);
              });
            } catch (e) {
              // pack.versions = pack.versions.reverse();
            }
          }
          pack.kind = 'modpack';
          logVerbose(rootState, 'Adding to cache', pack);
          commit('addToCache', pack);
          logVerbose(rootState, 'Resolving...', packID);
          resolve(pack);
        })
        .catch((err) => {
          console.error('Error getting modpack', err);
          reject('Pack is unavailable');
        });
    });
  },
  async refreshCache({ commit, rootState, dispatch }: any): Promise<any> {
    const packIDs = Object.keys(rootState.modpacks.packsCache);
    commit('clearCache');
    await Promise.all(
      packIDs.map(async (id: any) => {
        return await dispatch('fetchModpack', id);
      }),
    );
  },
};
