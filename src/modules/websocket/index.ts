import {Module} from 'vuex';
import {actions} from './actions';
import {getters} from './getters';
import {mutations} from './mutations';
import {SocketState} from './types';
import {RootState} from '@/types';

export const state: SocketState = {
  socket: {
    isConnected: false,
    message: '',
    reconnectError: false,
  },
  firstStart: true,
  messages: {},
  modal: null,
  reconnects: 0,
  pingEventCallback: undefined,
  ircEventCallback: undefined,
  exitCallback: undefined,
};

export const websocket: Module<SocketState, RootState> = {
  namespaced: false,
  state,
  actions,
  mutations,
  getters,
};
