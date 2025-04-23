import { defineStore } from 'pinia';
import { SettingsData } from '@/core/types/javaApi';
import { sendMessage } from '@/core/websockets/websocketsApi.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { constants } from '@/core/constants.ts';

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
  debugAdsDisabled?: boolean;
}

const debugSettingsKey = "app-debug-settings";

export const useAppSettings = defineStore("appSettings", {
  state: (): AppState => {
    const isProd = constants.isProduction
    let debugAdsDisabled = false;
    if (!isProd) {
      const debugSettings = localStorage.getItem(debugSettingsKey);
      if (debugSettings) {
        const settings = JSON.parse(debugSettings);
        debugAdsDisabled = settings?.debugAdsDisabled ?? false;
      }
    }
    
    return {
      rootSettings: null,
      rootSettingsLoading: false,
      systemHardware: null,
      debugAdsDisabled: debugAdsDisabled
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
    },
    
    toggleDebugAds() {
      const isProd = constants.isProduction
      if (isProd) {
        return
      }
      
      this.debugAdsDisabled = !this.debugAdsDisabled;
      localStorage.setItem(debugSettingsKey, JSON.stringify({ debugAdsDisabled: this.debugAdsDisabled }));
    }
  }
})