import {ActionContext, ActionTree} from 'vuex';
import {SocketState} from './types';
import {RootState} from '@/types';
import Vue from 'vue';
import {logVerbose} from '@/utils';
import platform from '@/utils/interface/electron-overwolf';

export interface MessageData {
  payload: any;
  callback: (data: any) => void;
}

export const actions: ActionTree<SocketState, RootState> = {
  sendMessage({ commit, rootState }: ActionContext<SocketState, RootState>, payload: MessageData) {
    if (Vue.prototype.$socket.readyState !== 1) {
      console.log(
        'Tried to send message whilst not connected properly',
        Vue.prototype.$socket.readyState,
        Vue.prototype.$socket,
      );
      return;
    }
    const requestId = platform.get.utils.crypto.randomUUID();
    payload.payload.requestId = requestId;
    payload.payload.secret = rootState.wsSecret;
    if (!(payload.payload?.type && payload.payload?.type !== '' && payload.payload?.type.startsWith('profiles.'))) {
      logVerbose(rootState, payload.payload);
    } else {
      logVerbose(rootState, 'Rejecting logging of profile data');
    }
    Vue.prototype.$socket[platform.isElectron() ? 'sendObj' : 'send'](
      platform.isElectron() ? payload.payload : JSON.stringify(payload.payload),
    ); // TODO: This conditional logic might be wrong
    commit('ADD_CALLBACK', { id: requestId, callback: payload.callback });
    return requestId;
  },
  disconnect() {
    Vue.prototype.$socket.close();
  },
  registerPingCallback({ commit }: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
    commit('ADD_PING_MESSAGE_CALLBACK', callback);
  },
  registerIRCCallback({ commit }: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
    commit('ADD_IRC_MESSAGE_CALLBACK', callback);
  },
  registerExitCallback({ commit }: ActionContext<SocketState, RootState>, callback: (data: any) => void) {
    commit('ADD_EXIT_CALLBACK', callback);
  },
};
