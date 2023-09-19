import { logVerbose } from '@/utils';
import Vue from 'vue';
import Vuex, { MutationTree, Store, StoreOptions } from 'vuex';
import { Alert, AlertWithId, ModalBox, RootState } from '@/types';
import { websocket } from './websocket';
import { settings } from './settings';
import { auth } from './auth';
// import { discovery } from './discovery';
import { servers } from './servers';
import { core } from '@/modules/core/core';
import { appStore } from '@/modules/app/appStore';
import {appStateStore} from '@/core/state/appState';
import {modpacks} from '@/modules/modpacks';

Vue.use(Vuex);

interface SecretMessage {
  port: number;
  secret: string;
}

export const mutations: MutationTree<RootState> = {
  SHOW_ALERT(state: any, alert: AlertWithId) {
    state.alerts.push(alert);
  },
  POP_ALERT(state: any) {
    state.alerts.shift();
  },
  CLEAR_ALERT(state: any, alertId: string) {
    state.alerts = state.alerts.filter((e: AlertWithId) => e.id !== alertId);
  },
  STORE_WS(state: any, data: SecretMessage) {
    state.wsPort = data.port;
    state.wsSecret = data.secret;
  },
  SHOW_MODAL(state: any, modal: ModalBox) {
    state.websocket.modal = modal;
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
    alerts: [],
    wsPort: 0,
    wsSecret: '',
  } as any,
  plugins: [wsLoggerPlugin],
  actions: {
    showAlert: ({ commit, rootState }: any, alert: Alert) => {
      const timeout = setTimeout(() => {
        commit('POP_ALERT');
      }, 5000);

      commit('SHOW_ALERT', { id: 'alert' + Math.random() * 400, timeout, ...alert });
    },
    hideAlert: ({ commit, rootState }: any, alert: AlertWithId) => {
      clearTimeout(alert.timeout);
      commit('CLEAR_ALERT', alert.id);
    },
    hideModal: ({ commit }: any) => {
      commit('HIDE_MODAL');
    },
  },
  mutations,
  modules: {
    modpacks,
    websocket,
    settings,
    auth,
    // discovery,
    servers,
    core,
    app: appStore,
    "v2": appStateStore
  },
};

export default new Vuex.Store<RootState>(store);
