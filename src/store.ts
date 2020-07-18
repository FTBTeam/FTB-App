import { logVerbose } from '@/utils';
import { ModpackState } from './modules/modpacks/types';
import Vue from 'vue';
import Vuex, { StoreOptions, MutationTree, Store } from 'vuex';
import { RootState, Alert, ModalBox } from './types';
import { news } from './modules/news';
import { modpacks } from './modules/modpacks';
import { websocket } from './modules/websocket';
import { settings } from './modules/settings';
import { auth } from './modules/auth';
import { discovery } from './modules/discovery';
import { servers } from './modules/servers';

Vue.use(Vuex);

interface SecretMessage {
    port: number;
    secret: string;
}


export const mutations: MutationTree<RootState> = {
    SHOW_ALERT(state: any, alert: Alert) {
        state.alert = alert;
    },
    CLEAR_ALERT(state: any) {
        state.alert = null;
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
        alert: null,
        wsPort: 0,
        wsSecret: '',
        modpacks: null,
        settings: null,
        auth: null,
    },
    plugins: [wsLoggerPlugin],
    actions: {
        showAlert: ({commit}: any, alert: Alert) => {
            commit('SHOW_ALERT', alert);
        },
        hideAlert: ({commit}: any) => {
            commit('CLEAR_ALERT');
        },
        hideModal: ({commit}: any) => {
            commit('HIDE_MODAL');
        },
    },
    mutations,
    modules: {
        news,
        modpacks,
        websocket,
        settings,
        auth,
        discovery,
        servers,
    },
};

export default new Vuex.Store<RootState>(store);
