import {GetterTree} from 'vuex';
import {ModpackState} from './types';
import {RootState} from '@/types';

export const getters: GetterTree<ModpackState, RootState> = {
    packsCache: (state: ModpackState) => {
        return state.packsCache ? state.packsCache : [];
    },
};
