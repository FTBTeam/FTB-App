import { Module } from 'vuex';
import { getters } from './getters';
import { actions } from './actions';
import {mutations} from './mutations';
import { AuthState } from './types';
import { RootState } from '@/types';

export const state: AuthState = {
    token: null,
    loading: false,
    error: false,
    friends: [],
    isFriendsWindowOpen: false,
    session: '',
    loggingIn: false,
};

export const auth: Module<AuthState, RootState> = {
    namespaced: true,
    state,
    getters,
    actions,
    mutations,
};
