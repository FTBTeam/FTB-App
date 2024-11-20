import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {initStateProcessor, lastMessageIndexByUuid} from '@/core/state/misc/runningStateProcessor';

initStateProcessor();

export type RunningState = typeof state;

/**
 * Shorthand vars to save on memory footprint
 */
export type InstanceMessageData = {
  i: number;
  t: string; 
  v: string;
}

export type Bar = {
  title: string;
  steps: number;
  step: number;
  message: string;
}

export type InstanceRunningData = {
  uuid: string;
  messages: InstanceMessageData[];
  preInitMessages: Set<string>;
  preInitProgress: {
    stepName: string;
    progress: number;
    steps: number;
    step: number;
    stepExtra?: string;
  },
  status: {
    crashed: boolean;
    finishedLoading: boolean;
  },
  startup: {
    step: {
      desc: string;
      totalSteps: number;
      step: number;
      progress: number;
      progressHuman: string;
      stepProgressHuman?: string;
    },
    bars?: Bar[] | null;
  }
}

export type LaunchingStatus = {
  uuid: string;
  loggingIn: boolean;
  starting: boolean;
  error: string;
  step: string;
}

const emptyStateData = {
  uuid: "",
  lastMessageType: '',
  messages: [],
  preInitMessages: new Set(),
  status: {
    crashed: false,
    finishedLoading: false,
  },
  preInitProgress: {
    stepName: '',
    progress: 0,
    steps: 0,
    step: 0,
  },
  startup: {
    step: {
      desc: '',
      totalSteps: 0,
      step: 0,
      progress: 0,
      progressHuman: '',
    },
    bars: null,
  }
}

function getOrCreateInstanceData(state: RunningState, uuid: string): InstanceRunningData {
  const data = state.instances.find((instance) => instance.uuid === uuid);
  if (data) {
    return data;
  }
  
  state.instances.push({
    ...(structuredClone(emptyStateData) as InstanceRunningData),
    messages: [],
    uuid: uuid
  });

  return state.instances.find((instance) => instance.uuid === uuid)!;
}

const state = {
  instances: [] as InstanceRunningData[],
  launchingStatus: null as LaunchingStatus | null
}

const actions: ActionTree<RunningState, RootState> = {
  async pushMessages({ commit }, payload: {uuid: string; message: InstanceMessageData[]}) {
    commit('PUSH_MESSAGES', payload);
  },
  async updateStatus({ commit }, payload: {uuid: string; step: InstanceRunningData['startup']['step']}) {
    commit('UPDATE_STATUS', payload);
  },
  async finishedLoading({ commit }, uuid: string) {
    commit('FINISHED_LOADING', uuid);
  },
  async updateBars({ commit }, payload: {uuid: string; bars: Bar[]}) {
    commit('UPDATE_BARS', payload);
  },
  async crashed({ commit }, uuid: string) {
    commit('CRASHED', uuid);
  },
  async stopped({ commit }, uuid: string) {
    commit('STOPPED', uuid);
  },
  async updateLaunchingStatus({ commit }, status: LaunchingStatus) {
    commit('UPDATE_LAUNCHING_STATUS', status);
  },
  async clearLaunchingStatus({ commit }) {
    commit('CLEAR_LAUNCHING_STATUS');
  }
}

const mutations: MutationTree<RunningState> = {
  PUSH_MESSAGES: (state: RunningState, payload: {uuid: string; messages: InstanceMessageData[]}) => {
    // Compare the current size vs the new size
    const stateMessages = getOrCreateInstanceData(state, payload.uuid).messages;
    const originalSize = stateMessages.length;
    const newSize = originalSize + payload.messages.length;
    
    if (newSize > 100_000) {
      // Remove the payload size from the start of the array
      stateMessages.splice(0, newSize - 100_000);
    }
    
    // Push the new messages
    stateMessages.push(...payload.messages);
  },
  UPDATE_STATUS: (state: RunningState, payload: {uuid: string; step: InstanceRunningData['startup']['step'], lastIndex: number}) => {
    const instanceData = getOrCreateInstanceData(state, payload.uuid);
    instanceData.startup.step = payload.step;
    
    const preInitMessages = instanceData.preInitMessages;
    if (!preInitMessages.has(payload.step.desc)) {
      preInitMessages.add(payload.step.desc);
      const lastMessageIndex = lastMessageIndexByUuid.get(payload.uuid) ?? 0;
      instanceData.messages.push({i: lastMessageIndex, t: "I", v: '[FTB APP][INFO] ' + payload.step.desc});
      lastMessageIndexByUuid.set(payload.uuid, lastMessageIndex + 1);
    }
    
    if (payload.step) {
      instanceData.preInitProgress = {
        stepName: payload.step.desc,
        // Progress is a float from 0 to 1, we want it to be a number from 0 to 100
        progress: payload.step.progress * 100,
        steps: payload.step.totalSteps,
        step: payload.step.step
      }
      
      if (payload.step.progressHuman) {
        instanceData.preInitProgress.stepExtra = payload.step.progressHuman;
      }
    }
  },
  FINISHED_LOADING: (state: RunningState, uuid: string) => {
    getOrCreateInstanceData(state, uuid).status.finishedLoading = true;
  },
  UPDATE_BARS: (state: RunningState, payload: {uuid: string; bars: Bar[]}) => {
    getOrCreateInstanceData(state, payload.uuid).startup.bars = payload.bars;
  },
  CRASHED: (state: RunningState, uuid: string) => {
    getOrCreateInstanceData(state, uuid).status.crashed = true;
  },
  STOPPED: (state: RunningState, uuid: string) => {
    // Remove the uuids data
    state.instances = state.instances.filter((instance) => instance.uuid !== uuid);
  },
  CLEAR_LAUNCHING_STATUS: (state: RunningState) => {
    state.launchingStatus = null;
  },
  UPDATE_LAUNCHING_STATUS: (state: RunningState, status: LaunchingStatus) => {
    state.launchingStatus = status;
  }
}

const getters: GetterTree<RunningState, RootState> = {
  instanceData: (state: RunningState) => (uuid: string): InstanceRunningData | undefined => state.instances.find((instance) => instance.uuid === uuid),
  launchingStatus: (state: RunningState) => state.launchingStatus,
  preInitProgress: (state: RunningState) => (uuid: string) => getOrCreateInstanceData(state, uuid).preInitProgress,
}

export const runningStateModule: Module<RunningState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}