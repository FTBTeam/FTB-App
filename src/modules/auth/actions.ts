import {ActionTree} from 'vuex';
import { AuthState } from './types';
import axios, {AxiosResponse} from 'axios';
import {RootState} from '@/types';
import { ipcRenderer } from 'electron';

export interface FriendRequestResponse {
    status: string;
    message: string;
    hash?: string;
}

export const actions: ActionTree<AuthState, RootState> = {
    logout({rootState, commit, dispatch}): void {
        commit('storeAuthDetails', null);
    },
    setWindow({rootState, commit}, data: boolean): void {
        commit('setFriendsWindow', data);
    },
    async setSessionID({rootState, commit}, payload: any): Promise<void> {
        commit('storeSession', payload);
        let response = await axios.get(`http://modpack-curator.ch.tools/api/me`, {headers: {
            Cookie: 'PHPSESSID=' + payload, Accept: "application/json"
        },
            withCredentials: true,
        });
        let user = response.data;
        if(user.accounts.find((s) => s.identityProvider === 'mcauth') !== undefined){
            let mc = user.accounts.find((s) => s.identityProvider === 'mcauth');
            try {
                const response = await axios.post(`https://api.creeper.host/minetogether/profile`, {target: mc.userName}, {headers: {'Content-Type': 'application/json'}});
                const profileResponse = response.data;
                user.mc = profileResponse.profileData[mc.userName];
            } catch (err) {
                console.error(err);
            }
        }
        ipcRenderer.send('user', user);
        commit('storeAuthDetails', user);
    },
    async getNewSession({rootState, commit}, payload: any): Promise<void> {
        let response = await axios.get(`http://modpack-curator.ch.tools/api/me`, {headers: {
                'App-Auth': payload, Accept: "application/json"
            }
        });
        let user = response.data;
        if(user.accounts.find((s) => s.identityProvider === 'mcauth') !== undefined){
            let mc = user.accounts.find((s) => s.identityProvider === 'mcauth');
            try {
                const response = await axios.post(`https://api.creeper.host/minetogether/profile`, {target: mc.userName}, {headers: {'Content-Type': 'application/json'}});
                const profileResponse = response.data;
                user.mc = profileResponse.profileData[mc.userName];
            } catch (err) {
                console.error(err);
            }
        }
        ipcRenderer.send('user', user);
        commit('storeAuthDetails', user);
        commit('storeSession', response.headers['app-token']);
    },
    storeAuthDetails({rootState, commit, dispatch}, payload: any): void {
        payload.friendCode = '';
        commit('storeAuthDetails', payload);
        console.log(payload);
        // dispatch('sendMessage', {payload: {type: 'storeAuthDetails', mpKey: payload.modpackskey, mpSecret: payload.modpackssecret}}, {root: true});
    },
    getFriends({rootState, commit, dispatch, state}, payload: any): Promise<void> {
        commit('setLoading', true);
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
        });
    },
    submitFriendRequest({rootState, commit, dispatch, state}, payload: {friendCode: string, display: string}): Promise<FriendRequestResponse> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/requestfriend`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash, target: payload.friendCode, display: payload.display})})
        .then((response) => response.json())
        .then(async (data) => {
            commit('setLoading', false);
            if (data.status === 'success') {
                return {
                    status: data.status,
                    message: data.message,
                    hash: data.hash,
                };
            } else {
                return {
                    status: data.status,
                    message: data.message,
                };
            }
        }).catch((err) => {
            commit('setLoading', false);
            return {
                status: 'error',
                message: 'unable to send friend request',
            };
        });
    },
    removeFriend({rootState, commit, dispatch, state}, payload: string): Promise<boolean | string> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/removeFriend`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash, target: payload})})
        .then((response) => response.json())
        .then(async (data) => {
            commit('setLoading', false);
            if (data.status === 'success') {
                return true;
            } else {
                return data.message;
            }
        }).catch((err) => {
            commit('setLoading', false);
            return 'Error sending request';
        });
    },
};
