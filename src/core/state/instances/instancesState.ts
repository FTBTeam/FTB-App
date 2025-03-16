import {sendMessage} from '@/core/websockets/websocketsApi';
import {InstanceJson} from '@/core/types/javaApi';
import {RootState} from '@/types';
import {createLogger} from '@/core/logger';

export type InstanceState = typeof state;

const state = {
  instances: [] as InstanceJson[],
  installingInstances: [] as string[],
  categories: [] as string[],
  state: {
    loadingInstances: false,
   }
}

const logger = createLogger("instances/instancesState.ts");

const actions: ActionTree<InstanceState, RootState> = {
  async loadInstances({state, commit}) {
    if (state.state.loadingInstances) {
      return;
    }
    
    commit('SET_LOADING_INSTANCES', true);
    logger.debug("Loading instances")
    try {
      const instances = await sendMessage('installedInstances', {refresh: true});
      logger.debug("Loaded instances")
      commit('SET_INSTANCES', instances.instances);
      commit('SET_CATEGORIES', instances.availableCategories);
    } catch (error) {
      logger.error("Failed to load instances", error);
    } finally {
      commit('SET_LOADING_INSTANCES', false);
    }
  },
  
  async addInstance({state, commit}, instance: InstanceJson) {
    logger.debug("Adding instance")
    commit('ADD_INSTANCE', instance);
    
    // Recalculate categories
    const categories = state.instances.map(i => i.category);
    commit('SET_CATEGORIES', Array.from(new Set(categories)));
  },
  
  async updateInstance({state, commit}, instance: InstanceJson) {
    logger.debug("Updating instance")
    const index = state.instances.findIndex(i => i.uuid === instance.uuid);
    if (index === -1) {
      logger.debug("Instance not found")
      return;
    }
    
    commit('UPDATE_INSTANCE', {index, instance});
    
    // Recalculate categories
    const categories = state.instances.map(i => i.category);
    commit('SET_CATEGORIES', Array.from(new Set(categories)));
  },
  
  async removeInstance({state, commit}, uuid: string) {
    logger.debug("Removing instance")
    const index = state.instances.findIndex(i => i.uuid === uuid);
    if (index === -1) {
      logger.debug("Instance not found for removal")
      return;
    }
    
    commit('REMOVE_INSTANCE', index);
  }
}

const mutations: MutationTree<InstanceState> = {
  SET_LOADING_INSTANCES: (state: InstanceState, loading: boolean) => state.state.loadingInstances = loading,
  SET_INSTANCES: (state: InstanceState, instances: InstanceJson[]) => state.instances = instances,
  ADD_INSTANCE: (state: InstanceState, instance: InstanceJson) => state.instances.push(instance),
  UPDATE_INSTANCE: (state: InstanceState, {index, instance}: {index: number, instance: InstanceJson}) => state.instances.splice(index, 1, instance),
  REMOVE_INSTANCE: (state: InstanceState, index: number) => state.instances.splice(index, 1),
  SET_CATEGORIES: (state: InstanceState, categories: string[]) => state.categories = categories,
  ADD_CATEGORY: (state: InstanceState, category: string) => state.categories.push(category),
}

const getters: GetterTree<InstanceState, RootState> = {
  isLoadingInstances: (state: InstanceState) => state.state.loadingInstances,
  instances: (state: InstanceState) => state.instances,
  getInstance: (state: InstanceState) => (uuid: string) => state.instances.find(i => i.uuid === uuid),
  categories: (state: InstanceState) => state.categories,
}

export type LoadInstancesFunction = (payload?: {refresh?: boolean}) => Promise<void>;
export type AddInstanceFunction = (instance: InstanceJson) => Promise<void>;

export const instanceStateModule: Module<InstanceState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}