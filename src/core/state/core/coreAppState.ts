import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {createLogger} from '@/core/logger';

export type CoreAppState = typeof state;

const state = {
  properties: {
    ready: false,
    installed: false,
  }
}

const logger = createLogger("coreAppState.ts");

const actions: ActionTree<CoreAppState, RootState> = {
  setReady({ commit }) {
    commit('SET_READY');
  },
  setInstalled({ commit }) {
    commit('SET_INSTALLED');
  }
}

const mutations: MutationTree<CoreAppState> = {
  SET_READY: (state) => state.properties.ready = true,
  SET_INSTALLED: (state) => state.properties.installed = true,
}

const getters: GetterTree<CoreAppState, RootState> = {
  ready: (state) => state.properties.ready,
  installed: (state) => state.properties.installed,
  completelyReady: (state) => state.properties.ready && state.properties.installed,
}

export const coreAppStateModule: Module<CoreAppState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}