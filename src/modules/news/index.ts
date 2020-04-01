import { Module } from 'vuex';
import { getters } from './getters';
import { actions } from './actions';
import {mutations} from './mutations';
import { NewsState } from './types';
import { RootState } from '@/types';

export const state: NewsState = {
    news: [],
    loading: false,
    error: false,
};

export const news: Module<NewsState, RootState> = {
    namespaced: true,
    state,
    getters,
    actions,
    mutations,
};
