import { MutationTree } from 'vuex';
import { SocketState } from './types';
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf';
import eventBus from '@/utils/event-bus';

export const mutations: MutationTree<SocketState> = {
  SOCKET_ONOPEN(state: any, event: any) {
    if (state.socket.reconnectError) {
      state.socket.reconnectError = false;
    }
    if (platform.isElectron()) {
      Vue.prototype.$socket = event.currentTarget;
    }
    state.socket.isConnected = true;
    state.firstStart = false;
  },
  SOCKET_ONCLOSE(state: any, event: any) {
    state.socket.isConnected = false;
  },
  SOCKET_ONERROR(state: any, event: any) {
    // console.error(state, event);
    state.firstStart = false;
  },
  SOCKET_ONMESSAGE(state: SocketState, message: any) {
    if (message.type !== 'ping' && message.type !== 'pong') {
      if (process.env.NODE_ENV === 'development') {
        const { requestId, type, ...rest } = message;
        console.info(
          `[${message.requestId ? ('' + message.requestId).padStart(6, '0') : '......'}][id//${message.type}]`,
          rest,
        );
      }
      eventBus.$emit('ws.message', message);
    }

    if (message.requestId) {
      if (state.messages[message.requestId]) {
        state.messages[message.requestId](message, message.requestId);
        if (
          message.type !== 'installInstanceDataReply' &&
          message.type !== 'installInstanceProgress' &&
          message.type !== 'updateInstanceProgress' &&
          message.type !== 'updateInstanceDataReply' &&
          message.type !== 'launchInstance.status'
        ) {
          delete state.messages[message.requestId];
        } else if (message.status === 'success') {
          delete state.messages[message.requestId];
          state.downloadedFiles = {};
        }
      }
    }
    if (message.type === 'ping') {
      if (state.pingEventCallback) {
        state.pingEventCallback(message);
      }
      if (!state.socket.isConnected) {
        state.socket.isConnected = true;
      }
    } else if (message.type === 'yeetLauncher') {
      if (state.exitCallback) {
        state.exitCallback(message);
      }
    } else if (message.type === 'openModal') {
      state.modal = message;
    } else if (message.type === 'closeModal') {
      state.modal = null;
    } else if (message.type === 'ircMessage') {
      if (state.ircEventCallback) {
        state.ircEventCallback(message);
      }
    } else if (message.type === 'install.filesEvent') {
      Object.keys(message.files).forEach((f: string) => {
        const status = message.files[f];
        Vue.set(state.downloadedFiles, f, status);
      });
      // Vue.set(state.downloadedFiles, message.fileName, message.status);
    }
    state.socket.message = message;
    platform.get.websocket.notifyWebhookReceived(message);
  },
  SOCKET_RECONNECT(state: any, count: number) {
    console.info(`Attempting to reconnect to java-backend, tries: ${count}`);
    state.reconnects = count;
  },
  SOCKET_RECONNECT_ERROR(state: any) {
    state.socket.reconnectError = true;
  },
  ADD_CALLBACK(state: any, data: any) {
    state.messages[data.id] = data.callback;
  },
  ADD_PING_MESSAGE_CALLBACK(state: any, callback: (data: any) => void) {
    state.pingEventCallback = callback;
  },
  ADD_IRC_MESSAGE_CALLBACK(state: any, callback: (data: any) => void) {
    state.ircEventCallback = callback;
  },
  ADD_EXIT_CALLBACK(state: any, callback: (data: any) => void) {
    state.exitCallback = callback;
  },
};
