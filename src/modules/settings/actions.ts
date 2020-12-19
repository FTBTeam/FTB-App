import { ipcRenderer } from 'electron';
import {ActionTree} from 'vuex';
import {SettingsState, Settings, JavaVersions} from './types';
import {RootState} from '@/types';

export const actions: ActionTree<SettingsState, RootState> = {
    loadSettings({dispatch, commit}) {
        return new Promise((resolve, reject) => {
            dispatch('sendMessage', {payload: {type: 'getSettings'}, callback: (msg: any) => {
                if (msg.settingsInfo.blockedUesrs !== undefined && !Array.isArray(msg.settingsInfo.blockedUesrs)) {
                    msg.settingsInfo.blockedUsers = JSON.parse(msg.settingsInfo.blockedUsers);
                }
                commit('loadSettings', msg.settingsInfo);
                ipcRenderer.send('updateSettings', msg.settingsInfo);
                commit('loadHardware', msg);
                resolve();
            }}, {root: true});
        });
    },
    saveSettings({dispatch, commit}, settings: Settings) {
        if (Array.isArray(settings.blockedUsers)) {
            settings.blockedUsers = JSON.stringify(settings.blockedUsers);
        }
        dispatch('sendMessage', {payload: {type: 'saveSettings', settingsInfo: settings}, callback: () => {
            dispatch('loadSettings');
        }}, {root: true});
    },
    loadJavaVersions({dispatch, commit}) {
        dispatch('sendMessage', {payload: {type: 'getJavas'}, callback: (data: {javas: JavaVersions}) => {
            commit('loadVersions', data.javas);
        }}, {root: true});
    },
};
