import Vue from 'vue';
import Vuex, { StoreOptions } from 'vuex';
import { RootState } from './types';
import { news } from './modules/news';
import { modpacks } from './modules/modpacks';
import { websocket } from './modules/websocket';
import { settings } from './modules/settings';

Vue.use(Vuex);

const store: StoreOptions<RootState> = {
    state: {
        version: '1.0.0',
    },
    modules: {
        news,
        modpacks,
        websocket,
        settings,
    },
};

export default new Vuex.Store<RootState>(store);
