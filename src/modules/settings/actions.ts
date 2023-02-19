import { ActionTree } from 'vuex';
import { Settings, SettingsState } from './types';
import { RootState } from '@/types';
import platform from '@/utils/interface/electron-overwolf';
import {setExtra} from '@sentry/vue';

export const actions: ActionTree<SettingsState, RootState> = {
  loadSettings({ dispatch, commit }) {
    return new Promise((resolve, reject) => {
      dispatch(
        'sendMessage',
        {
          payload: { type: 'getSettings' },
          callback: (msg: any) => {
            if (msg.settingsInfo.blockedUesrs !== undefined && !Array.isArray(msg.settingsInfo.blockedUesrs)) {
              msg.settingsInfo.blockedUsers = JSON.parse(msg.settingsInfo.blockedUsers);
            }

            const settings = msg.settingsInfo;
            Object.keys(settings).forEach((key: string) => {
              if (key === 'listMode' && settings[key] === undefined) {
                settings[key] = false;
              }
              if ((settings as any)[key] === 'true') {
                (settings as any)[key] = true;
              } else if ((settings as any)[key] === 'false') {
                (settings as any)[key] = false;
              } else if (!isNaN(parseInt((settings as any)[key], 10))) {
                (settings as any)[key] = parseInt((settings as any)[key], 10);
              }
            });

            commit('loadSettings', settings);
            platform.get.actions.updateSettings(msg.settingsInfo);
            commit('loadHardware', msg);
            resolve(null);
          },
        },
        { root: true },
      );
    });
  },
  saveSettings({ dispatch, commit }, settings: Settings) {
    if (Array.isArray(settings.blockedUsers)) {
      settings.blockedUsers = JSON.stringify(settings.blockedUsers);
    }
    dispatch(
      'sendMessage',
      {
        payload: { type: 'saveSettings', settingsInfo: settings },
        callback: () => {
          dispatch('loadSettings');
        },
      },
      { root: true },
    );
  },
};
