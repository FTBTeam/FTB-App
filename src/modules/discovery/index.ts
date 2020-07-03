import { Module } from 'vuex';
import { getters } from './getters';
import { actions } from './actions';
import {mutations} from './mutations';
import {  DiscoveryState } from './types';
import { RootState } from '@/types';

export const state: DiscoveryState = {
    discoveryQueue: [],
    loading: false,
    error: false,
};

export const discovery: Module<DiscoveryState, RootState> = {
    namespaced: true,
    state,
    getters,
    actions,
    mutations,
};
