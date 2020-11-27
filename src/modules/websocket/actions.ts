import {ActionTree, Store, ActionContext} from 'vuex';
import {SocketState} from './types';
import {RootState} from '@/types';
import Vue from 'vue';
import { logVerbose } from '@/utils';

export interface MessageData {
  payload: any;
  callback: (data: any) => void;
}

export const actions: ActionTree<SocketState, RootState> = {
    sendMessage({commit, rootState}: ActionContext<SocketState, RootState>, payload: MessageData) {
        const messageID = Math.round(Math.random() * 1000);
        payload.payload.requestId = messageID;
        payload.payload.secret = rootState.wsSecret;
        logVerbose(rootState, payload.payload);
        Vue.prototype.$socket.sendObj(payload.payload);
        commit('ADD_CALLBACK', {id: messageID, callback: payload.callback});
    },
    registerModProgressCallback({commit}: ActionContext<SocketState, RootState>, callback: (data: any) => void){
        commit('ADD_MOD_PROGRESS_CALLBACK', callback);
    },
};
