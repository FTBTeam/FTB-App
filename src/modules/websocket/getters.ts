import {GetterTree} from 'vuex';
import {SocketState} from './types';
import {RootState} from '@/types';

export const getters: GetterTree<SocketState, RootState> = {
    getFileStatus: (state: SocketState) => (name: string) => {
        return state.downloadedFiles[name] || 'waiting';
    },
};
