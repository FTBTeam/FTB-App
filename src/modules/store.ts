import {logVerbose} from '@/utils';
import Vue from 'vue';
import Vuex, {MutationTree, Store, StoreOptions} from 'vuex';
import {RootState} from '@/types';
import {websocket} from './websocket';
import {settings} from './settings';
import {auth} from './auth';
import {core} from '@/modules/core/core';
import {appStore} from '@/modules/app/appStore';
import {modpackStateModule} from '@/core/state/modpacks/modpacksState';
import {instanceStateModule} from '@/core/state/instances/instancesState';
import {installStateModule} from '@/core/state/instances/installState';
import {newsStateModule} from '@/core/state/misc/newsState';
import {dialogsState} from '@/core/state/misc/dialogsState';
import {adsStateModule} from '@/core/state/misc/adsState';

Vue.use(Vuex);

interface SecretMessage {
  port: number;
  secret: string;
}

export const mutations: MutationTree<RootState> = {
  STORE_WS(state: any, data: SecretMessage) {
    state.wsPort = data.port;
    state.wsSecret = data.secret;
  },
  HIDE_MODAL(state: any) {
    state.websocket.modal = null;
  },
};

const wsLoggerPlugin = (store: Store<RootState>) => {
  store.subscribe((mutation, state) => {
    if (mutation.type === 'SOCKET_ONMESSAGE') {
      logVerbose(state, 'New WebSocket message', JSON.stringify(mutation.payload));
    }
  });
};

const store: StoreOptions<RootState> = {
  state: {
    version: '1.0.0',
    wsPort: 0,
    wsSecret: '',
  } as any,
  plugins: [wsLoggerPlugin],
  actions: {
    hideModal: ({ commit }: any) => {
      commit('HIDE_MODAL');
    },
  },
  mutations,
  modules: {
    websocket,
    settings,
    auth,
    // discovery,
    // servers,
    core,
    app: appStore,
    "v2/modpacks": modpackStateModule,
    "v2/instances": instanceStateModule,
    "v2/install": installStateModule,
    "v2/news": newsStateModule,
    "v2/dialogs": dialogsState,
    "v2/ads": adsStateModule
  },
};

export default new Vuex.Store<RootState>(store);
