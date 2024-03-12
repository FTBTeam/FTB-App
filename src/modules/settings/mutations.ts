import {MutationTree} from 'vuex';
import {Resolution, SettingsState} from './types';
import {SettingsData} from '@/core/@types/javaApi';

export const mutations: MutationTree<SettingsState> = {
  loadSettings(state, payload: SettingsData) {
    state.settings = { ...payload };
  },
  saveSettings(state, payload: SettingsData) {
    state.settings = payload;
  },
  loadHardware(state, payload: any) {
    state.hardware = {
      totalMemory: payload.totalMemory,
      totalCores: payload.totalCores,
      availableMemory: payload.availableMemory,
      mainScreen: payload.mainScreen as Resolution,
      supportedResolutions:
        payload.supportedResolutions.length > 0
          ? (payload.supportedResolutions.sort((a: Resolution, b: Resolution) => {
              return b.width + b.height - (a.width + a.height);
            }) as Resolution[])
          : [],
    };
  },
  updateSetting() {},
};
