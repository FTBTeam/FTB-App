import { defineStore } from 'pinia';
import { SettingsData } from '@/core/types/javaApi';
import { sendMessage } from '@/core/websockets/websocketsApi.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';

type AppState = {
  connecting: boolean;
  ready: boolean;
  backendSettings: SettingsData | null;
  backendSettingsLoading: boolean;
}

export const useAppStore = defineStore("app", {
  state: (): AppState => {
    return {
      connecting: false,
      ready: false,
      
      backendSettings: null,
      backendSettingsLoading: false
    }
  },
  
  actions: {
    async loadSettings() {
      const settings = await toggleBeforeAndAfter(async () => await sendMessage("getSettings"), v => this.backendSettingsLoading = v)
      if (settings) {
        this.backendSettings = settings.settingsInfo;
      }
    }
  }
})