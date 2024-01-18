import {ActionContext, ActionTree} from 'vuex';
import {SocketState} from './types';
import {RootState} from '@/types';
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf';
import {createLogger} from '@/core/logger';

export interface MessageData {
  payload: any;
  callback: (data: any) => void;
}

const logger = createLogger("websocket/actions.ts");

export const actions: ActionTree<SocketState, RootState> = {
  sendMessage({ commit, rootState }: ActionContext<SocketState, RootState>, payload: MessageData) {
    if (Vue.prototype.$socket.readyState !== 1) {
      logger.warn(
        'Tried to send message whilst not connected properly',
        Vue.prototype.$socket.readyState,
        Vue.prototype.$socket,
      );
      return;
    }
    
    const requestId = platform.get.utils.crypto.randomUUID();
    payload.payload.requestId = requestId;
    payload.payload.secret = rootState.wsSecret;
    Vue.prototype.$socket.sendObj(payload.payload);
    
    commit('ADD_CALLBACK', { id: requestId, callback: payload.callback });
    return requestId;
  },
  connected() {
    
  },
  disconnect() {
    Vue.prototype.$socket.close();
  },
  registerPingCallback({ commit }: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
    commit('ADD_PING_MESSAGE_CALLBACK', callback);
  },
  registerIRCCallback({ commit }: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
    commit('ADD_IRC_MESSAGE_CALLBACK', callback);
  }
};
