import Vue from 'vue';
import Vuex, {MutationTree, StoreOptions} from 'vuex';
import {RootState} from '@/types';
import {websocket} from './websocket';
import {settings} from './settings';
import {core} from '@/modules/core/core';
import {modpackStateModule} from '@/core/state/modpacks/modpacksState';
import {instanceStateModule} from '@/core/state/instances/instancesState';
import {installStateModule} from '@/core/state/instances/installState';
import {newsStateModule} from '@/core/state/misc/newsState';
import {dialogsState} from '@/core/state/misc/dialogsState';
import {adsStateModule} from '@/core/state/misc/adsState';
import {coreAppStateModule} from '@/core/state/core/coreAppState';
import {mtAuthStateModule} from '@/core/state/core/mtAuthState';
import {apiCredentialsStateModule} from '@/core/state/core/apiCredentialsState';

Vue.use(Vuex);

export const mutations: MutationTree<RootState> = {
  HIDE_MODAL(state: any) {
    state.websocket.modal = null;
  },
};

const store: StoreOptions<RootState> = {
  state: {} as any,
  actions: {
    hideModal: ({ commit }: any) => {
      commit('HIDE_MODAL');
    },
  },
  mutations,
  modules: {
    core,
    websocket,
    settings,
    // discovery,
    // servers,
    "v2/app": coreAppStateModule,
    "v2/modpacks": modpackStateModule,
    "v2/instances": instanceStateModule,
    "v2/install": installStateModule,
    "v2/news": newsStateModule,
    "v2/dialogs": dialogsState,
    "v2/ads": adsStateModule,
    "v2/mtauth": mtAuthStateModule,
    "v2/apiCredentials": apiCredentialsStateModule
  }
};

export default new Vuex.Store<RootState>(store);
