import Vue from 'vue';
import Vuex, { StoreOptions,MutationTree } from 'vuex';
import { RootState, Alert } from './types';
import { news } from './modules/news';
import { modpacks } from './modules/modpacks';
import { websocket } from './modules/websocket';
import { settings } from './modules/settings';

Vue.use(Vuex);


export const mutations: MutationTree<RootState> = {
    SHOW_ALERT(state: any, alert: Alert){
        state.alert = alert;
    },
    CLEAR_ALERT(state: any){
        state.alert = null;
    }
};


const store: StoreOptions<RootState> = {
    state: {
        version: '1.0.0',
        alert: null
    },
    actions: {
        "showAlert": ({commit}: any, alert: Alert) => {
            commit('SHOW_ALERT', alert);
        },
        "hideAlert": ({commit}: any) => {
            commit('CLEAR_ALERT');
        }
    },
    mutations,
    modules: {
        news,
        modpacks,
        websocket,
        settings,
    },
};

export default new Vuex.Store<RootState>(store);
