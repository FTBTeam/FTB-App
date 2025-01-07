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
import {coreAppStateModule} from '@/core/state/core/coreAppState';
import {apiCredentialsStateModule} from '@/core/state/core/apiCredentialsState';
import {userFavouritesModule} from '@/core/state/misc/userFavouritesState';
import {runningStateModule} from '@/core/state/misc/runningState';

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
    "v2/app": coreAppStateModule,
    "v2/modpacks": modpackStateModule,
    "v2/instances": instanceStateModule,
    "v2/install": installStateModule,
    "v2/news": newsStateModule,
    "v2/dialogs": dialogsState,
    "v2/apiCredentials": apiCredentialsStateModule,
    "v2/userFavourites": userFavouritesModule,
    "v2/running": runningStateModule
  }
};

export default new Vuex.Store<RootState>(store);
