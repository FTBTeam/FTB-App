import {MutationTree} from 'vuex';
import { ServersState} from './types';

export const mutations: MutationTree<ServersState> = {
    setLoading(state, payload) {
        state.loading = payload;
    },
    loadServers(state, payload) {
        state.servers = payload;
    }
}
