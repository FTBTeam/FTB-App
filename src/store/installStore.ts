import { defineStore } from 'pinia';
import { InstallRequest, InstallStatus } from '@/core/controllers/InstanceInstallController.ts';

import {ModLoaderUpdateState} from '@/core/types/appTypes.ts';

export type InstallState = {
  installQueue: InstallRequest[],
  currentInstall: InstallStatus | null,
  currentModloaderUpdate: ModLoaderUpdateState[]
}

export const useInstallStore = defineStore("install", {
  state: (): InstallState => {
    return {
      installQueue: [],
      currentInstall: null,
      currentModloaderUpdate: []
    }
  },

  actions: {
    popInstallQueue() {
      if (this.installQueue.length == 0) {
        return null;
      }

      return this.installQueue.shift();
    },
    
    addModloaderUpdate(payload: ModLoaderUpdateState) {
      this.currentModloaderUpdate.push(payload);
    },
    
    removeModloaderUpdate(packetId: string) {
      this.currentModloaderUpdate = this.currentModloaderUpdate.filter(e => e.packetId !== packetId);
    }
  }
})