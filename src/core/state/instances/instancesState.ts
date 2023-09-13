import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {AppState} from '@/core/state/appState';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {InstanceJson} from '@/core/@types/javaApi';
import {RootState} from '@/types';

export type InstanceState = typeof state;

const state = {
  instances: [] as InstanceJson[],
  installingInstances: [] as string[],
  state: {
    loadingInstances: false,
    instancesInitialized: false,
  }
}

const actions: ActionTree<InstanceState, RootState> = {
  async loadInstances({state, commit}) {
    if (state.state.loadingInstances) {
      return;
    }
    
    commit('SET_LOADING_INSTANCES', true);
    // TODO: We might not always want to refresh
    const instances = await sendMessage('installedInstances', {refresh: true});
    commit('SET_INSTANCES', instances.instances);
    commit('SET_INSTANCES_INITIALIZED', true);
    commit('SET_LOADING_INSTANCES', false);
  },
  
  async addInstance({state, commit}, instance: InstanceJson) {
    commit('ADD_INSTANCE', instance);
  },
  
  async updateInstance({state, commit}, instance: InstanceJson) {
    const index = state.instances.findIndex(i => i.uuid === instance.uuid);
    if (index === -1) {
      return;
    }
    
    commit('UPDATE_INSTANCE', {index, instance});
  },
  
  async removeInstance({state, commit}, instance: InstanceJson) {
    const index = state.instances.findIndex(i => i.uuid === instance.uuid);
    if (index === -1) {
      return;
    }
    
    commit('REMOVE_INSTANCE', index);
  }
}

const mutations: MutationTree<InstanceState> = {
  SET_LOADING_INSTANCES: (state: InstanceState, loading: boolean) => state.state.loadingInstances = loading,
  SET_INSTANCES: (state: InstanceState, instances: InstanceJson[]) => state.instances = instances,
  SET_INSTANCES_INITIALIZED: (state: InstanceState, initialized: boolean) => state.state.instancesInitialized = initialized,
  ADD_INSTANCE: (state: InstanceState, instance: InstanceJson) => state.instances.push(instance),
  UPDATE_INSTANCE: (state: InstanceState, {index, instance}: {index: number, instance: InstanceJson}) => state.instances.splice(index, 1, instance),
  REMOVE_INSTANCE: (state: InstanceState, index: number) => state.instances.splice(index, 1),
}

const getters: GetterTree<InstanceState, RootState> = {
  isLoadingInstances: (state: InstanceState) => state.state.loadingInstances,
  instancesInitialized: (state: InstanceState) => state.state.instancesInitialized,
  instances: (state: InstanceState) => state.instances,
  getInstance: (state: InstanceState) => (uuid: string) => state.instances.find(i => i.uuid === uuid),
}

export const instanceStateModule: Module<InstanceState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}