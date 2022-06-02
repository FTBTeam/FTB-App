import { Module } from 'vuex';
import { RootState } from '@/types';
import { AppStoreMutations, AppStoreState, InstallerState } from '@/modules/app/appStore.types';
import { wsTimeoutWrapper } from '@/utils';

export const appStore: Module<AppStoreState, RootState> = {
  namespaced: true,
  state: {
    pack: {
      currentlyRunning: null,
    },
    installer: null,
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
    installer(state: AppStoreState): InstallerState | null {
      return state.installer;
    },
  },
  actions: {
    setRunningPack({ commit }): void {
      commit(AppStoreMutations.SET_RUNNING_PACK);
    },

    async installModpack({ commit }, request: InstallerState) {
      const pack = request.pack;

      // Prepare the request, we're either updating, using a share code or installing fresh
      let payload: any = {};
      if (pack.shareCode) {
        payload = {
          shareCode: pack.shareCode,
        };
      } else if (pack.importFrom) {
        payload = {
          importFrom: pack.importFrom,
        };
      } else if (pack.uuid && pack.version) {
        payload = {
          uuid: pack.uuid,
          version: pack.version,
          id: pack.id,
        };
      } else {
        payload = {
          id: pack.id,
          version: pack.version,
        };
      }

      if (pack.packType) {
        payload['packType'] = pack.packType;
      }

      if (pack.private) {
        payload['_private'] = true;
      }

      // This event brings back lots of request, so instead we just assume the first one is either a
      // success or error
      const rest = await wsTimeoutWrapper({
        type: pack.uuid && pack.version ? 'updateInstance' : 'installInstance',
        ...payload,
      });

      if (pack.uuid && pack.version) {
        request = { ...request, meta: { ...request.meta, isUpdate: true } };
      }

      commit(AppStoreMutations.INSTALL_PACK, request);
    },

    clearInstaller({ commit }) {
      commit(AppStoreMutations.INSTALL_PACK, null);
    },
  },
  mutations: {
    setRunningPack(state, payload: string | null): void {
      state.pack.currentlyRunning = payload;
    },

    installPack(state, payload): void {
      state.installer = payload;
    },
  },
};
