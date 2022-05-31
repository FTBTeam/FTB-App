import { Module } from 'vuex';
import { RootState } from '@/types';
import { AppStoreMutations, AppStoreState, InstallingState } from '@/modules/app/appStore.types';

export const appStore: Module<AppStoreState, RootState> = {
  namespaced: true,
  state: {
    pack: {
      currentlyRunning: null,
    },
    installing: null,
  },
  getters: {
    /**
     * The pack that's running like, right now!
     */
    getRunningPack: (state: AppStoreState): string | null => {
      return state.pack.currentlyRunning;
    },

    /**
     * Installer
     */
    installingPack(state: AppStoreState): InstallingState | null {
      return state.installing;
    },
  },
  actions: {
    setRunningPack({ commit }): void {
      commit(AppStoreMutations.SET_RUNNING_PACK);
    },

    installModpack({ commit }, data: InstallingState) {
      commit(AppStoreMutations.INSTALL_PACK, data);
    },
  },
  mutations: {
    setRunningPack(state, payload: string | null): void {
      state.pack.currentlyRunning = payload;
    },

    installPack(state, payload): void {
      state.installing = payload;
    },
  },
};
