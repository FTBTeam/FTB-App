import { Module } from 'vuex';
import { RootState } from '@/types';
import { AppStoreMutations, AppStoreState } from '@/modules/app/appStore.types';

export const appStore: Module<AppStoreState, RootState> = {
  namespaced: true,
  state: {
    pack: {
      currentlyRunning: null,
    },
  },
  getters: {
    /**
     * The pack that's running like, right now!
     */
    getRunningPack: (state: AppStoreState): string | null => {
      return state.pack.currentlyRunning;
    },
  },
  actions: {
    setRunningPack({ commit }): void {
      commit(AppStoreMutations.SET_RUNNING_PACK);
    },
  },
  mutations: {
    setRunningPack(state, payload: string | null): void {
      state.pack.currentlyRunning = payload;
    },
  },
};
