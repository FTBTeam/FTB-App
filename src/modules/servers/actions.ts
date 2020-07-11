import {ActionTree} from 'vuex';
import { ServersState } from './types';
import {RootState} from '@/types';

export const actions: ActionTree<ServersState, RootState> = {
    getFriends({rootState, commit, dispatch, state}): Promise<void> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/list`, {headers: {
            'Content-Type': 'application/json',
        }})
        .then((response) => response.json())
        .then(async (data) => {
            const servers = data.servers;
            commit('loadServers', servers);
            commit('setLoading', false);
        }).catch((err) => {
            commit('setLoading', false);
            console.error(err);
        });
    },
};
