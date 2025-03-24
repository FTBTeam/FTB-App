import { defineStore } from 'pinia';
import { SettingsData } from '@/core/types/javaApi';
import { sendMessage } from '@/core/websockets/websocketsApi.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';

export interface Resolution {
  width: number;
  height: number;
}

export interface Hardware {
  totalMemory: number;
  totalCores: number;
  availableMemory: number;
  supportedResolutions: Resolution[];
}

type AppState = {
  rootSettings: SettingsData | null;
  rootSettingsLoading: boolean;
  systemHardware: Hardware | null;
}

export const useAppSettings = defineStore("appSettings", {
  state: (): AppState => {
    return {
      rootSettings: null,
      rootSettingsLoading: false,
      systemHardware: null
    }
  },

  actions: {
    async loadSettings() {
      const settings = await toggleBeforeAndAfter(async () => await sendMessage("getSettings"), v => this.rootSettingsLoading = v)
      if (settings) {
        this.rootSettings = settings.settingsInfo;
        this.systemHardware = {
          totalMemory: settings.totalMemory,
          totalCores: settings.totalCores,
          availableMemory: settings.availableMemory,
          supportedResolutions: settings.supportedResolutions
        }
      }
    },
    
    async loadIfNeeded() {
      if (!this.rootSettings) {
        await this.loadSettings();
      }
    },
    
    async saveSettings(settings: SettingsData) {
      const saveResult = await sendMessage("saveSettings", { settings })
      if (saveResult.settings) {
        this.rootSettings = saveResult.settings;
      }
    }
  }
})