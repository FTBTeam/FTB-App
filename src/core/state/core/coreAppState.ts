import {RootState} from '@/types';

export type CoreAppState = typeof state;

const state = {
  wsSecret: null as string | null,
  properties: {
  }
}

const actions: ActionTree<CoreAppState, RootState> = {
  storeWsSecret({commit}, wsSecret: string) {
    commit('STORE_WS_SECRET', wsSecret);
  }
}

const mutations: MutationTree<CoreAppState> = {
  STORE_WS_SECRET(state, wsSecret: string) {
    state.wsSecret = wsSecret;
  }
}

const getters: GetterTree<CoreAppState, RootState> = {
  wsSecret(state) {
    return state.wsSecret;
  }
}

export const coreAppStateModule: Module<CoreAppState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}