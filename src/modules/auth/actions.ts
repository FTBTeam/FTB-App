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
        const settings = rootState.settings?.settings;
        if(settings !== undefined){
            if(settings.sessionString !== undefined && settings.sessionString.length > 0){
                settings.sessionString = "";
                dispatch('settings/saveSettings', settings, {root: true});
            }
        }
    },
    setWindow({rootState, commit}, data: boolean): void {
        commit('setFriendsWindow', data);
    },
    async setSessionID({rootState, commit, dispatch}, payload: any): Promise<void> {
        commit('storeSession', payload);
        commit('startLoggingIn');
        let response;
        try {
            response = await axios.get(`https://minetogether.io/api/me`, {headers: {
                Cookie: 'PHPSESSID=' + payload, Accept: "application/json"
            },
                withCredentials: true,
            });
        }catch(err){
            commit('loggedIn');
            return;
        }
        const user = response.data;
        if(user.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined){
            const mc = user.accounts.find((s: any) => s.identityProvider === 'mcauth');
            try {
                const response = await axios.post(`https://api.creeper.host/minetogether/profile`, {target: mc.userName}, {headers: {'Content-Type': 'application/json'}});
                const profileResponse = response.data;
                user.mc = profileResponse.profileData[mc.userName];
            } catch (err) {
                console.error(err);
                commit('loggedIn');
            }
        }
        ipcRenderer.send('user', user);
        dispatch('storeAuthDetails', user);
        commit('loggedIn');
    },
    async getNewSession({rootState, commit, dispatch}, payload: any): Promise<void> {
        commit('startLoggingIn');
        let response;
        try {
            response = await axios.get(`https://minetogether.io/api/me`, {headers: {
                    'App-Auth': payload, Accept: "application/json"
                }
            });
        }catch(err){
            commit('loggedIn');
            return;
        }
        const user = response.data;
        if(user.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined){
            const mc = user.accounts.find((s: any) => s.identityProvider === 'mcauth');
            try {
                const response = await axios.post(`https://api.creeper.host/minetogether/profile`, {target: mc.userName}, {headers: {'Content-Type': 'application/json'}});
                const profileResponse = response.data;
                user.mc = profileResponse.profileData[mc.userName];
            } catch (err) {
                console.error(err);
                commit('loggedIn');
            }
        }
        ipcRenderer.send('user', user);
        dispatch('storeAuthDetails', user);
        commit('storeSession', response.headers['app-token']);
        ipcRenderer.send('session', response.headers['app-token']);
        commit('loggedIn');
    },
    storeAuthDetails({rootState, commit, dispatch}, payload: any): void {
        payload.friendCode = '';
        commit('storeAuthDetails', payload);
        if(payload === null){
            dispatch('sendMessage', {payload: {type: 'storeAuthDetails', mpKey: "", mpSecret: "", s3Bucket: "", s3Host: "", s3Key: "", s3Secret: "", mtHash: ""}}, {root: true});            
        } else {
            let s3Bucket = "", s3Host = "", s3Key = "", s3Secret = "";
            let mpKey = "";
            if(payload.activePlan !== null && payload.activePlan !== undefined){
                const fields = payload.activePlan.customFields.customfield;
                s3Bucket = fields.find((f: any) => f.name === "S3 Bucket").value;
                s3Host = fields.find((f: any) => f.name === "S3 Server").value;
                s3Key = fields.find((f: any) => f.name === "S3 Key").value;
                s3Secret = fields.find((f: any) => f.name === "S3 Secret").value;
            }
            if(payload.attributes['modpackschkey'] !== undefined){
                mpKey = payload.attributes['modpackschkey'][0];
            }
            let mtHash = "";
            if(payload.mc !== undefined && payload.mc.hash !== undefined && payload.mc.hash.long !== undefined){
                mtHash = payload.mc.hash.long
            }
            dispatch('sendMessage', {payload: {type: 'storeAuthDetails', mpKey: mpKey, mpSecret: "", s3Bucket, s3Host, s3Key, s3Secret, mtHash}}, {root: true});
            dispatch('sendMessage', {payload: {type: 'installedInstances', refresh: true}, callback: function(data: any){
                dispatch('modpacks/storeInstalledPacks', data, {root: true})
            }}, {root: true});
            dispatch('modpacks/getPrivatePacks', {}, {root: true})
            if(rootState.settings?.settings.enableChat){            
                dispatch('connectToIRC')
            }
        }
    },
    getFriends({rootState, commit, dispatch, state}, payload: any): Promise<void> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/listfriend`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash.long})})
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
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash.long})})
        .then((response) => response.json())
        .then(async (data) => {
            commit('setFriendCode', data.code);
            commit('setLoading', false);
        }).catch((err) => {
            commit('setLoading', false);
        });
    },
    submitFriendRequest({rootState, commit, dispatch, state}, payload: {friendCode: string; display: string}): Promise<FriendRequestResponse> {
        commit('setLoading', true);
        return fetch(`https://api.creeper.host/minetogether/requestfriend`, {headers: {
            'Content-Type': 'application/json',
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash.long, target: payload.friendCode, display: payload.display})})
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
        }, method: 'POST', body: JSON.stringify({hash: state.token?.mc.hash.long, target: payload})})
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
    async connectToIRC({state, commit, dispatch}){
        let response;
        try {
            response = await axios.get(`https://api.creeper.host/minetogether/chatserver`, {headers: { Accept: "application/json"}});
        }catch(err){
            return;
        }
        const server = response.data;
        dispatch('sendMessage', {payload: {type: "ircConnect", host: server.server.address, port: server.server.port, nick: state.token?.mc.chat.hash.medium, realname: JSON.stringify({p: ""})}}, {root: true})
    }
};
