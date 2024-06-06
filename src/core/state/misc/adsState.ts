import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {Ad} from '@/core/@types/external/metaApi.types';

export type AdsState = typeof state;

const state = {
  activeAds: [] as Ad[],
}

const actions: ActionTree<AdsState, RootState> = {
  async sync({ commit}, ads: Ad[]) {
    commit('SET_ADS', ads);
  },
}

const mutations: MutationTree<AdsState> = {
  SET_ADS: (state: AdsState, ads: Ad[]) => state.activeAds = ads,
}

const getters: GetterTree<AdsState, RootState> = {
  ads: (state: AdsState): Ad[] => state.activeAds,
}

export const adsStateModule: Module<AdsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}