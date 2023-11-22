import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {InstallRequest, InstallStatus} from '@/core/controllers/InstanceInstallController';
import {RootState} from '@/types';
import {ModLoaderUpdateState} from '@/core/@types/states/appState';

export type InstallState = typeof state;

const state = {
  installQueue: [] as InstallRequest[],
  currentInstall: null as InstallStatus | null,
  currentModloaderUpdate: [] as ModLoaderUpdateState[]
}

const actions: ActionTree<InstallState, RootState> = {
  // Pull the top most entry from the queue and return it
  async popInstallQueue({state, commit}) {
    if (state.installQueue.length == 0) {
      return null;
    }
    
    return state.installQueue.shift();
  },
  
  addModloaderUpdate({state, commit}, payload: ModLoaderUpdateState) {
    commit("ADD_MODLOADER_UPDATE", payload);
  },
  
  removeModloaderUpdate({state, commit}, packetId: string) {
    commit("REMOVE_MODLOADER_UPDATE", packetId);
  }
}

const mutations: MutationTree<InstallState> = {
  SET_CURRENT_INSTALL: (state: InstallState, status: InstallStatus | null) => state.currentInstall = status,
  ADD_MODLOADER_UPDATE: (state: InstallState, payload: ModLoaderUpdateState) => state.currentModloaderUpdate.push(payload),
  REMOVE_MODLOADER_UPDATE: (state: InstallState, packetId: string) => state.currentModloaderUpdate = state.currentModloaderUpdate.filter(e => e.packetId !== packetId),
}

const getters: GetterTree<InstallState, RootState> = {
  currentInstall: (state: InstallState) => state.currentInstall,
  installQueue: (state: InstallState) => state.installQueue,
  currentModloaderUpdate: (state: InstallState) => state.currentModloaderUpdate,
}

export const installStateModule: Module<InstallState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}