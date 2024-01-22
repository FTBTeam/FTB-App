import {ActionTree} from 'vuex';
import {SettingsState} from './types';
import {RootState} from '@/types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {SettingsData} from '@/core/@types/javaApi';

export const actions: ActionTree<SettingsState, RootState> = {
  async loadSettings({ dispatch, commit }) {
    const result = await sendMessage("getSettings", {});

    const settings = result.settingsInfo
    
    commit('loadSettings', settings);
    commit('loadHardware', result);
  },
  async saveSettings({ dispatch, commit }, settings: SettingsData) {
    await sendMessage('saveSettings', {
      settings: settings
    })
    
    dispatch('loadSettings');
  },
};
