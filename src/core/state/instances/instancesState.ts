import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {AppState} from '@/core/state/appState';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {InstanceJson} from '@/core/@types/javaApi';

export type InstanceState = typeof state;

const state = {
  instances: [] as InstanceJson[],
  installingInstances: [] as string[],
  state: {
    loadingInstances: false,
    instancesInitialized: false,
  }
}

const actions: ActionTree<InstanceState, AppState> = {
  async loadInstances({state, commit}) {
    if (state.state.loadingInstances) {
      return;
    }
    
    commit('SET_LOADING_INSTANCES', true);
    const instances = await sendMessage('installedInstances', {refresh: false});
    commit('SET_INSTANCES', instances.instances);
    commit('SET_INSTANCES_INITIALIZED', true);
    commit('SET_LOADING_INSTANCES', false);
  }
}

const mutations: MutationTree<InstanceState> = {
  SET_LOADING_INSTANCES: (state: InstanceState, loading: boolean) => state.state.loadingInstances = loading,
  SET_INSTANCES: (state: InstanceState, instances: InstanceJson[]) => state.instances = instances,
  SET_INSTANCES_INITIALIZED: (state: InstanceState, initialized: boolean) => state.state.instancesInitialized = initialized,
}

const getters: GetterTree<InstanceState, AppState> = {
  isLoadingInstances: (state: InstanceState) => state.state.loadingInstances,
  instancesInitialized: (state: InstanceState) => state.state.instancesInitialized,
  instances: (state: InstanceState) => state.instances,
}

export const instanceStateModule: Module<InstanceState, AppState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}