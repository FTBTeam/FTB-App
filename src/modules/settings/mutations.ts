import {MutationTree} from 'vuex';
import {Resolution, Settings, SettingsState} from './types';

const defaultSettings: Settings = {
  width: 1720,
  height: 840,
  memory: 3072,
  enablePreview: false,
  jvmargs: '',
  exitOverwolf: false,
  enableChat: true, 
  threadLimit: 2,
  speedLimit: 0,
  cacheLife: 5184000,
  instanceLocation: '',
  verbose: false,
  autoOpenChat: true,
  blockedUsers: [],
  showAdverts: true,
  proxyPort: -1,
  proxyType: 'none',
  proxyHost: '',
  proxyPassword: '',
  proxyUser: '',
  updateChannel: 'release',
  fullScreen: false,
  useSystemWindowStyle: false,
  shellArgs: '',
};

export const mutations: MutationTree<SettingsState> = {
  loadSettings(state, payload: Settings) {
    state.settings = { ...defaultSettings, ...payload };
  },
  saveSettings(state, payload: Settings) {
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
