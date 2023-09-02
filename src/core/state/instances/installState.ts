import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {AppState} from '@/core/state/appState';
import {InstallRequest, InstallStatus} from '@/core/controllers/InstanceInstallController';
import {RootState} from '@/types';

export type InstallState = typeof state;

const state = {
  installQueue: [] as InstallRequest[],
  currentInstall: null as InstallStatus | null,
}

const actions: ActionTree<InstallState, RootState> = {
  // Pull the top most entry from the queue and return it
  async popInstallQueue({state, commit}) {
    if (state.installQueue.length == 0) {
      return null;
    }
    
    return state.installQueue.shift();
  }
}

const mutations: MutationTree<InstallState> = {
  SET_CURRENT_INSTALL: (state: InstallState, status: InstallStatus | null) => state.currentInstall = status,
}

const getters: GetterTree<InstallState, RootState> = {
  currentInstall: (state: InstallState) => state.currentInstall,
}

export const installStateModule: Module<InstallState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}