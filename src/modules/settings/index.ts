import {Module} from 'vuex';
import {getters} from './getters';
import {actions} from './actions';
import {mutations} from './mutations';
import {SettingsState} from './types';
import {RootState} from '@/types';

export const state: SettingsState = {
    settings: {
        width: 0,
        height: 0,
        memory: 0,
        keepLauncherOpen: false,
        jvmargs: '',
        enableAnalytics: false,
        enableBeta: false,
        threadLimit: 2,
        speedLimit: 0,
        cacheLife: 5184000,
        packCardSize: 2,
        instanceLocation: "",
    },
    error: false,
    hardware: {
        totalCores: 0,
        totalMemory: 0,
        availableMemory: 0,
        mainScreen: {
            width: 0,
            height: 0,
        },
        supportedResolutions: [],
    },
};

export const settings: Module<SettingsState, RootState> = {
    namespaced: true,
    state,
    getters,
    actions,
    mutations,
};
