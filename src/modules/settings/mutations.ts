import {MutationTree} from 'vuex';
import {SettingsState, Settings, Resolution} from './types';

export const mutations: MutationTree<SettingsState> = {
    loadSettings(state, payload: Settings) {
        const defaultSettings: Settings = {
            width: 1720,
            height: 840,
            memory: 3072,
            keepLauncherOpen: true,
            enablePreview: false,
            jvmargs: '',
            enableAnalytics: true,
            enableChat: true,
            enableBeta: false,
            threadLimit: 2,
            speedLimit: 0,
            cacheLife: 5184000,
            packCardSize: 1,
            instanceLocation: '',
            listMode: false,
            verbose: false,
            cloudSaves: false,
            autoOpenChat: true,
            blockedUsers: [],
            mtConnect: false,
            automateMojang: true,
            showAdverts: true,
            loadInApp: true
        };
        state.settings = {...defaultSettings, ...payload};
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
            supportedResolutions: payload.supportedResolutions.length > 0 ? payload.supportedResolutions.sort((a: Resolution, b: Resolution) => {
                return (b.width + b.height) - (a.width + a.height);
              }) as Resolution[] : [],
        };
    },
    loadVersions(state, payload: any){
        state.javaInstalls = payload;
    }
};
