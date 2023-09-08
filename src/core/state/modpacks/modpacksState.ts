import {ActionTree, ActionContext, GetterTree, Module, MutationTree} from 'vuex';
import {ModPack} from '@/modules/modpacks/types';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {RootState} from '@/types';

export type ModpackState = typeof state;

const state = {
  modpacks: new Map<number, ModPack>(),
  // Shorthold modpacks store is intended for modpacks that we have no need to keep in memory for a long period of time.
  // To is primarily used for searching.
  shortHoldModpacks: new Map<number, ModPack>(), // TODO: Maybe remove
  featuredPackIds: [] as number[],
  latestPackIds: [] as number[],
}

async function getModpackIds(type: "featured" | "latest", {state, commit}: ActionContext<ModpackState, RootState>, limit = 5): Promise<number[]> {
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

  const featuredPacks = req.packs.sort((a, b) => b - a)
  commit(endpoint.store, featuredPacks);
  return featuredPacks;
}

const actions: ActionTree<ModpackState, RootState> = {
  /**
   * Get a modpack from the API or from the store if it already exists.
   */
  async getModpack({state, commit}, id: number) {
    if (state.modpacks.has(id)) {
      return state.modpacks.get(id)!;
    }
    
    const req = await modpackApi.modpacks.getModpack(id);
    if (req == null || req.status !== "success") {
      return null;
    }
    
    const modpack = req as ModPack
    if (modpack) {
      commit('SET_MODPACK', modpack);
    }
    
    return modpack;
  },
  
  async getFeaturedPacks(context) {
    return getModpackIds("featured", context);
  },
  
  async getLatestModpacks(context) {
    return getModpackIds("latest", context, 10);
  }
}

const mutations: MutationTree<ModpackState> = {
  SET_MODPACK: (state: ModpackState, modpack: ModPack) => state.modpacks.set(modpack.id, modpack),
  SET_FEATURED_PACKS: (state: ModpackState, packs: number[]) => state.featuredPackIds = packs,
  SET_LATEST_PACKS: (state: ModpackState, packs: number[]) => state.latestPackIds = packs,
}

const getters: GetterTree<ModpackState, RootState> = {
  featuredPacks: (state: ModpackState) => state.featuredPackIds,
  latestPacks: (state: ModpackState) => state.latestPackIds,
  getApiPack: (state: ModpackState) => (id: number) => state.modpacks.get(id),
}

export type GetModpack = (id: number) => Promise<ModPack | null>;

export const modpackStateModule: Module<ModpackState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}