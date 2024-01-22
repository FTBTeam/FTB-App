import {Module} from 'vuex';
import {getters} from './getters';
import {actions} from './actions';
import {mutations} from './mutations';
import {SettingsState} from './types';
import {RootState} from '@/types';
import {SettingsData} from '@/core/@types/javaApi';

export const state: SettingsState = {
  settings: {} as SettingsData,
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
