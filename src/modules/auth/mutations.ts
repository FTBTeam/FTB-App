import {MutationTree} from 'vuex';
import {AuthState} from './types';

export const mutations: MutationTree<AuthState> = {
    storeAuthDetails(state, payload){
        state.token = payload;

    }
};
