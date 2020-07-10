import {ActionTree} from 'vuex';
import { AuthState } from './types';
import {RootState} from '@/types';
export const actions: ActionTree<AuthState, RootState> = {
    storeAuthDetails({rootState, commit, dispatch}, payload: any): void {
        payload.friendCode = '';
        commit('storeAuthDetails', payload);
        dispatch('sendMessage', {payload: {type: 'storeAuthDetails', mpKey: payload.modpackskey, mpSecret: payload.modpackssecret}}, {root: true});
    },
    getFriends({rootState, commit, dispatch, state}, payload: any): Promise<void> {
        commit('setLoading', true);
        console.log(state.token);
        return fetch(`https://api.creeper.host/minetogether/listfriend`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash})})
        .then((response) => response.json())
        .then(async (data) => {
            const friends = data.friends;
            commit('loadFriends', friends);
            commit('setLoading', false);
        }).catch((err) => {
            commit('setLoading', false);
            console.error(err);
        });
    },
    getFriendCode({rootState, commit, dispatch, state}, payload: any): Promise<void> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/friendcode`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash})})
        .then((response) => response.json())
        .then(async (data) => {
            commit('setFriendCode', data.code);
            commit('setLoading', false);
        }).catch((err) => {
            commit('setLoading', false);
            console.error(err);
        });
    },
    submitFriendRequest({rootState, commit, dispatch, state}, payload: {friendCode: string, display: string}): Promise<any> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/requestfriend`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash, target: payload.friendCode, display: payload.display})})
        .then((response) => response.json())
        .then(async (data) => {
            console.log(data);
            commit('setLoading', false);
        }).catch((err) => {
            commit('setLoading', false);
            console.error(err);
        });
    },
};
