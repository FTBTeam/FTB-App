import { ModPack } from './../modpacks/types';
import {MutationTree} from 'vuex';
import { DiscoveryState } from './types';

export const mutations: MutationTree<DiscoveryState> = {
    setLoading(state, payload: boolean) {
        state.loading = payload;
    },
    loadQueue(state, payload: ModPack[]) {
        state.discoveryQueue = payload;
    },
    addToQueue(state, payload: ModPack) {
        state.discoveryQueue.push(payload);
    },
    queueError(state) {
        state.error = true;
        if (state.discoveryQueue.length === 0) {
            state.discoveryQueue = [];
        }
        state.loading = false;
    },
};
