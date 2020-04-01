import {MutationTree} from 'vuex';
import {SettingsState, Settings, Resolution} from './types';

export const mutations: MutationTree<SettingsState> = {
    loadSettings(state, payload: Settings) {
        state.settings = payload;
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
};
