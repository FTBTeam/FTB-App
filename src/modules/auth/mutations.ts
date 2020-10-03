import {MutationTree} from 'vuex';
import {AuthState} from './types';

export const mutations: MutationTree<AuthState> = {
    storeSession(state, payload) {
        state.session = payload;
    },
    storeAuthDetails(state, payload) {
        state.token = payload;
    },
    setLoading(state, payload) {
        state.loading = payload;
    },
    loadFriends(state, payload) {
        state.friends = payload;
    },
    setFriendCode(state, payload) {
        if (state.token !== null) {
            state.token.mc.friendCode = payload;
        }
    },
    setFriendsWindow(state, payload){
        state.isFriendsWindowOpen = payload;
    },
    startLoggingIn(state){
        state.loggingIn = true;
    },
    loggedIn(state){
        state.loggingIn = false;
    }
};
