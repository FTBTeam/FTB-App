import {MutationTree} from 'vuex';
import {SocketState} from './types';
// @ts-ignore
import Vue from 'vue';

export const mutations: MutationTree<SocketState> = {
    SOCKET_ONOPEN(state: any, event: any)  {
        Vue.prototype.$socket = event.currentTarget;
        state.socket.isConnected = true;
        state.firstConnect = false;
    },
    SOCKET_ONCLOSE(state: any, event: any)  {
        state.socket.isConnected = false;
    },
    SOCKET_ONERROR(state: any, event: any)  {
        // console.error(state, event);
        state.firstConnect = false;
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
        console.log("MessageType: ", message.type);
        if(message.type === "openModal"){
            console.log("Opening modal");
            state.modal = message;
            console.log("Opened modal", state.modal);
        } else if (message.type === "closeModal"){
            state.modal = null;
        }
        state.socket.message = message;
        console.log(JSON.stringify(message));
    },
    SOCKET_RECONNECT(state: any, count: number) {
        console.info(`Attempting to reconnect to java-backend, tries: ${count}`);
    },
    SOCKET_RECONNECT_ERROR(state: any) {
        state.socket.reconnectError = true;
    },
    ADD_CALLBACK(state: any, data: any) {
        state.messages[data.id] = data.callback;
    },
};
