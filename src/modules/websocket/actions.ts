import {ActionTree} from 'vuex';
import {SocketState} from './types';
import {RootState} from '@/types';
import Vue from 'vue';

export interface MessageData {
  payload: any;
  callback: (data: any) => void;
}

export const actions: ActionTree<SocketState, RootState> = {
    sendMessage(context: any, payload: MessageData) {
        const messageID = Math.round(Math.random() * 1000);
        payload.payload.requestId = messageID;
        Vue.prototype.$socket.sendObj(payload.payload);
        context.commit('ADD_CALLBACK', {id: messageID, callback: payload.callback});
    },
};
