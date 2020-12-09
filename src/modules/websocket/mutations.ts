import { ipcRenderer } from 'electron';
import {MutationTree} from 'vuex';
import {SocketState} from './types';
// @ts-ignore
import Vue from 'vue';

export const mutations: MutationTree<SocketState> = {
    SOCKET_ONOPEN(state: any, event: any)  {
        if(state.socket.reconnectError){
            state.socket.reconnectError = false;
        }
        Vue.prototype.$socket = event.currentTarget;
        state.socket.isConnected = true;
        state.firstStart = false;
    },
    SOCKET_ONCLOSE(state: any, event: any)  {
        state.socket.isConnected = false;
    },
    SOCKET_ONERROR(state: any, event: any)  {
        // console.error(state, event);
        state.firstStart = false;
    },
    SOCKET_ONMESSAGE(state: any, message: any)  {
        if (message.requestId) {
            if (state.messages[message.requestId]) {
                state.messages[message.requestId](message);
                if (message.type !== 'installInstanceDataReply' && message.type !== 'installInstanceProgress' && message.type !== 'updateInstanceProgress' && message.type !== 'updateInstanceDataReply') {
                    delete state.messages[message.requestId];
                } else if (message.status === 'success') {
                    delete state.messages[message.requestId];
                }
            }
        }
        if (message.type === 'ping') {
            if(state.pingEventCallback){
                state.pingEventCallback(message);
            }
        } else if (message.type === 'openModal') {
            state.modal = message;
        } else if (message.type === 'closeModal') {
            state.modal = null;
        }
        else if (message.type === 'clientLaunchData') {
            if (state.modProgressCallback) {
                state.modProgressCallback(message);
            }
        }
        state.socket.message = message;
        ipcRenderer.send("websocketReceived", message);
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
    ADD_MOD_PROGRESS_CALLBACK(state: any, callback: (data: any) => void){
        state.modProgressCallback = callback;
    },
    ADD_PING_MESSAGE_CALLBACK(state: any, callback: (data: any) => void){
        state.pingEventCallback = callback;
    },
};
