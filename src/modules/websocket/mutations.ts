import {MutationTree} from 'vuex';
import {SocketState} from './types';
import Vue from 'vue';
import platform from '@/utils/interface/electron-overwolf'

export const mutations: MutationTree<SocketState> = {
    SOCKET_ONOPEN(state: any, event: any)  {
        if (state.socket.reconnectError) {
            state.socket.reconnectError = false;
        }
        Vue.prototype.$socket = event.currentTarget;
        state.socket.isConnected = true;
        state.firstStart = false;
        console.log(Vue.prototype.$socket);
    },
    SOCKET_ONCLOSE(state: any, event: any)  {
        state.socket.isConnected = false;
    },
    SOCKET_ONERROR(state: any, event: any)  {
        // console.error(state, event);
        state.firstStart = false;
    },
    SOCKET_ONMESSAGE(state: SocketState, message: any)  {
        if (message.requestId) {
            if (state.messages[message.requestId]) {
                state.messages[message.requestId](message);
                if (message.type !== 'installInstanceDataReply' && message.type !== 'installInstanceProgress' && message.type !== 'updateInstanceProgress' && message.type !== 'updateInstanceDataReply') {
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
        } else if (message.type === 'openModal') {
            state.modal = message;
        } else if (message.type === 'closeModal') {
            state.modal = null;
        } else if (message.type === 'ircEvent') {
            if (state.ircEventCallback) {
                state.ircEventCallback(message);
            }
        } else if (message.type === 'clientLaunchData') {
            if (state.modProgressCallback) {
                state.modProgressCallback(message);
            }
        } else if (message.type === 'installedFileEventDataReply') {
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
    ADD_MOD_PROGRESS_CALLBACK(state: any, callback: (data: any) => void) {
        state.modProgressCallback = callback;
    },
    ADD_EXIT_CALLBACK(state: any, callback: (data: any) => void) {
      state.exitCallback = callback;
    }
};
