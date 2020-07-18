import {MutationTree} from 'vuex';
import { ServersState, Server} from './types';

export const mutations: MutationTree<ServersState> = {
    setLoading(state, payload) {
        state.loading = payload;
    },
    loadServers(state, payload) {
        state.servers[payload.id] = payload.servers;
    },
    updateServer(state, payload) {
        if (state.servers[payload.id]) {
            const servers = state.servers[payload.id];
            let server = servers.find((s) => s.id === payload.server.id);
            if (server !== undefined) {
                const i = servers.indexOf(server);
                server = payload.server as Server;
                servers[i] = server;
            }
            state.servers[payload.id] = servers;
        }
    },
};
