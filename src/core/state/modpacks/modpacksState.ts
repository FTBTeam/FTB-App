import {ActionContext, ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {ModPack, ModpackVersion, PackProviders, Versions} from '@/modules/modpacks/types';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {RootState} from '@/types';

export type ModpackState = typeof state;

export const packBlacklist = [
  81,  // Vanilla
  104, // Forge
  105, // Fabric
  116  // NeoForge
]

const state = {
  modpacks: new Map<number, ModPack>(),
  modpackVersions: new Map<number, Versions>(),
  featuredPackIds: [] as number[],
  latestPackIds: [] as number[],
}

async function getModpackIds(type: "featured" | "latest", {state, commit}: ActionContext<ModpackState, RootState>, limit = 5, ignoreBlacklist = false): Promise<number[]> {
  const endpoints = {
    featured: {
      endpoint: () => modpackApi.modpacks.getFeaturedPacks(limit),
      store: "SET_FEATURED_PACKS",
      existing: "featuredPackIds"
    },
    latest: {
      endpoint: () => modpackApi.modpacks.getLatestPacks(limit),
      store: "SET_LATEST_PACKS",
      existing: "latestPackIds"
    }
  }
  
  const endpoint = endpoints[type];
  if ((state as any)[endpoint.existing].length > 0) {
    return (state as any)[endpoint.existing];
  }
  
  const req = await endpoint.endpoint();
  if (!req) {
    return [];
  }

  let featuredPacks = req.packs.sort((a, b) => b - a)
  if (!ignoreBlacklist) {
    featuredPacks = featuredPacks.filter(e => !packBlacklist.includes(e));
  }
  
  commit(endpoint.store, featuredPacks);
  return featuredPacks;
}

const actions: ActionTree<ModpackState, RootState> = {
  /**
   * Get a modpack from the API or from the store if it already exists.
   */
  async getModpack({state, commit}, payload: {
    id: number,
    provider?: PackProviders
  }) {
    const {id, provider} = payload;
    
    if (state.modpacks.has(id)) {
      return state.modpacks.get(id)!;
    }
    
    const req = await modpackApi.modpacks.getModpack(id, provider ?? "modpacksch");
    if (req == null || req.status !== "success") {
      return null;
    }
    
    const modpack = req as ModPack
    if (modpack) {
      commit('SET_MODPACK', modpack);
    }
    
    return modpack;
  },
  
  async getVersion({state, commit}, payload: {
    id: number,
    versionId: number,
    provider?: PackProviders
  }) {
    const {id, versionId, provider} = payload;

    if (state.modpackVersions.has(versionId)) {
      return state.modpackVersions.get(versionId)!;
    }

    const req = await modpackApi.modpacks.getModpackVersion(id, versionId, provider ?? "modpacksch");
    if (req == null || req.status !== "success") {
      return null;
    }

    const modpackVersion = req
    if (modpackVersion) {
      commit('SET_MODPACK_VERSION', modpackVersion);
    }

    return modpackVersion;
  },
  
  async getFeaturedPacks(context) {
    return getModpackIds("featured", context);
  },
  
  async getLatestModpacks(context) {
    return getModpackIds("latest", context, 15);
  },
  
  clearModpacks({state, commit}) {
    commit('CLEAR_MODPACKS');
    commit('CLEAR_VERSIONS');
  }
}

const mutations: MutationTree<ModpackState> = {
  SET_MODPACK: (state: ModpackState, modpack: ModPack) => state.modpacks.set(modpack.id, modpack),
  CLEAR_MODPACKS: (state: ModpackState) => state.modpacks.clear(),
  CLEAR_VERSIONS: (state: ModpackState) => state.modpackVersions.clear(),
  SET_FEATURED_PACKS: (state: ModpackState, packs: number[]) => state.featuredPackIds = packs,
  SET_LATEST_PACKS: (state: ModpackState, packs: number[]) => state.latestPackIds = packs,
  SET_MODPACK_VERSION: (state: ModpackState, version: Versions) => state.modpackVersions.set(version.id, version),
}

const getters: GetterTree<ModpackState, RootState> = {
  featuredPacks: (state: ModpackState) => state.featuredPackIds,
  latestPacks: (state: ModpackState) => state.latestPackIds,
  getApiPack: (state: ModpackState) => (id: number) => state.modpacks.get(id),
}

export type GetModpack = (payload: {
  id: number, provider?: PackProviders
}) => Promise<ModPack | null>;

export type GetModpackVersion = (payload: {
  id: number,
  versionId: number,
  provider?: PackProviders
}) => Promise<ModpackVersion | null>;

export const modpackStateModule: Module<ModpackState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}