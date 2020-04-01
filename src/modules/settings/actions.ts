import {ActionTree} from 'vuex';
import {SettingsState, Settings} from './types';
import {RootState} from '@/types';

export const actions: ActionTree<SettingsState, RootState> = {
    loadSettings({dispatch, commit}) {
        return new Promise((resolve, reject) => {
            dispatch('sendMessage', {payload: {type: 'getSettings'}, callback: (msg: any) => {
                commit('loadSettings', msg.settingsInfo);
                commit('loadHardware', msg);
                resolve();
            }}, {root: true});
        });
    },
    saveSettings({dispatch, commit}, settings: Settings) {
        dispatch('sendMessage', {payload: {type: 'saveSettings', settingsInfo: settings}, callback: () => {
            dispatch('loadSettings');
        }}, {root: true});
    },
};
