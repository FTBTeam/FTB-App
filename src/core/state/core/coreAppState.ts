import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {createLogger} from '@/core/logger';

export type CoreAppState = typeof state;

const state = {
  properties: {
  }
}

const logger = createLogger("coreAppState.ts");

const actions: ActionTree<CoreAppState, RootState> = {
}

const mutations: MutationTree<CoreAppState> = {
}

const getters: GetterTree<CoreAppState, RootState> = {
}

export const coreAppStateModule: Module<CoreAppState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}