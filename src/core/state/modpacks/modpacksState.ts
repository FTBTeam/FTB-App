import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {AppState} from '@/core/state/appState';
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
  
  async getFeaturedPacks({state, commit}) {
    if (state.featuredPackIds.length > 0) {
      return state.featuredPackIds;
    }
    
    const req = await modpackApi.modpacks.getFeaturedPacks();
    if (!req) {
      return [];
    }
    
    const featuredPacks = req.packs.sort((a, b) => b - a)
    commit('SET_FEATURED_PACKS', featuredPacks);
    return featuredPacks;
  }
}

const mutations: MutationTree<ModpackState> = {
  SET_MODPACK: (state: ModpackState, modpack: ModPack) => state.modpacks.set(modpack.id, modpack),
  SET_FEATURED_PACKS: (state: ModpackState, packs: number[]) => state.featuredPackIds = packs
}

const getters: GetterTree<ModpackState, RootState> = {
  featuredPacks: (state: ModpackState) => state.featuredPackIds,
}

export type GetModpack = (id: number) => Promise<ModPack | null>;

export const modpackStateModule: Module<ModpackState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}