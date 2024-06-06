import {MutationTree} from 'vuex';
import {SocketState} from './types';
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf';
import {emitter} from '@/utils/event-bus';
import {createLogger} from '@/core/logger';

const logger = createLogger("websocket/mutations.ts");

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
    logger.debug("Socket closed", event)
  },
  SOCKET_ONERROR(state: any, event: any) {
    logger.error(state, event);
    state.firstStart = false;
  },
  SOCKET_ONMESSAGE(state: SocketState, message: any) {
    if (message.type !== 'ping' && message.type !== 'pong') {
      const { requestId, type, ...rest } = message;
      if (message.type !== "launchInstance.logs" && !message?.notViableForLogging) {
        // Done like this to avoid being seen when searching for the normal version
        console["debug"](`WS Message: [${message.requestId ?? 'unknown'}::${message.type}]`, rest)
      } else if (message.notViableForLogging) {
        console["debug"](`WS Message: [${message.requestId ?? 'unknown'}::${message.type}]`, "Not viable for logging")
      }
      emitter.emit('ws.message', message);
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
    } else if (message.type === 'openModal') {
      state.modal = message;
    } else if (message.type === 'closeModal') {
      state.modal = null;
    }
    state.socket.message = message;
  },
  SOCKET_RECONNECT(state: any, count: number) {
    logger.info(`Attempting to reconnect to java-backend, tries: ${count}`);
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
  }
};
