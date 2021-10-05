import { Module } from 'vuex';
import { getters } from './getters';
import { actions } from './actions';
import {mutations} from './mutations';
import { ServersState } from './types';
import { RootState } from '@/types';

export const state: ServersState = {
    loading: false,
    error: false,
    servers: {},
};

export const servers: Module<ServersState, RootState> = {
    namespaced: true,
    state,
    getters,
    actions,
    mutations,
};
