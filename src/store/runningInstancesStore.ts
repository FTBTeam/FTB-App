import { defineStore } from 'pinia';
import { lastMessageIndexByUuid } from '@/core/controllers/runningStateProcessor.ts';

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

const emptyStateData: InstanceRunningData = {
  uuid: "",
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

type RunningInstancesState = {
  instances: InstanceRunningData[];
  launchingStatus: LaunchingStatus | null;
}

export const useRunningInstancesStore = defineStore("runningInstances", {
  state: (): RunningInstancesState => {
    return {
      instances: [],
      launchingStatus: null
    }
  },

  actions: {
    pushMessages(uuid: string, messages: InstanceMessageData[]) {
      // Compare the current size vs the new size
      const stateMessages = getInstanceData(this.instances, uuid, (data) => {
        this.instances.push(data);
        return data;
      }).messages;
      
      const originalSize = stateMessages.length;
      const newSize = originalSize + messages.length;

      if (newSize > 100_000) {
        // Remove the payload size from the start of the array
        stateMessages.splice(0, newSize - 100_000);
      }

      // Push the new messages
      stateMessages.push(...messages);
    },
    
    updateStatus(uuid: string, step: InstanceRunningData['startup']['step']) {
      const instanceData = getInstanceData(this.instances, uuid, (data) => {
        this.instances.push(data);
        return data;
      })
  
      instanceData.startup.step = step;
    
      const preInitMessages = instanceData.preInitMessages;
      if (!preInitMessages.has(step.desc)) {
        preInitMessages.add(step.desc);
        const lastMessageIndex = lastMessageIndexByUuid.get(uuid) ?? 0;
        instanceData.messages.push({i: lastMessageIndex, t: "I", v: '[FTB APP][INFO] ' + step.desc});
        lastMessageIndexByUuid.set(uuid, lastMessageIndex + 1);
      }
    
      if (step) {
        instanceData.preInitProgress = {
          stepName: step.desc,
          // Progress is a float from 0 to 1, we want it to be a number from 0 to 100
          progress: step.progress * 100,
          steps: step.totalSteps,
          step: step.step
        }
    
        if (step.progressHuman) {
          instanceData.preInitProgress.stepExtra = step.progressHuman;
        }
      }
    },
    
    finishedLoading(uuid: string) {
      getInstanceData(this.instances, uuid, (data) => {
        this.instances.push(data);
        return data;
      }).status.finishedLoading = true;
    },
    
    updateBars(uuid: string, bars: Bar[] | undefined) {
      getInstanceData(this.instances, uuid, (data) => {
        this.instances.push(data);
        return data;
      }).startup.bars = bars;
    },
    
    crashed(uuid: string) {
      getInstanceData(this.instances, uuid, (data) => {
        this.instances.push(data);
        return data;
      }).status.crashed = true;
    },
    
    stopped(uuid: string) {
      // Remove the uuids data
      this.instances = this.instances.filter((instance) => instance.uuid !== uuid);
    },
    
    clearLaunchingStatus() {
      this.launchingStatus = null;
    },
    
    updateLaunchingStatus(status: LaunchingStatus) {
      this.launchingStatus = status;
    }
  }
})

function getInstanceData(instances: InstanceRunningData[], uuid: string, onCreate: (data: InstanceRunningData) => InstanceRunningData): InstanceRunningData {
  const data = instances.find((instance) => instance.uuid === uuid);
  if (data) {
    return data;
  }

  return onCreate({
    ...(structuredClone(emptyStateData) as InstanceRunningData),
    messages: [],
    uuid: uuid
  });
}