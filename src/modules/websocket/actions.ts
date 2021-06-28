import {ActionTree, ActionContext} from 'vuex';
import {SocketState} from './types';
import {RootState} from '@/types';
import Vue from 'vue';
import { logVerbose } from '@/utils';
import axios from 'axios';

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
    registerPingCallback({commit}: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
        commit('ADD_PING_MESSAGE_CALLBACK', callback);
    },
    registerIRCCallback({commit}: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
      commit('ADD_IRC_MESSAGE_CALLBACK', callback);
    },
    registerModProgressCallback({commit}: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
      commit('ADD_MOD_PROGRESS_CALLBACK', callback);
    },
    async reportAdvert({commit, rootState}: ActionContext<SocketState, RootState>, data: {html: string, object: string}) {
        try {
          const response = await axios.post(`https://minetogether.io/api/reportAd`, data, {headers: {
              Accept: 'application/json',
          }});
      } catch (err) {
          console.log(err);
          return;
      }
    },
};
