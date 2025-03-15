import {RootState} from '@/types';
import {constants} from '@/core/constants';

export type ApiCredentialsState = typeof state;

const defaultCredentials = {
}

const state = {
  ...defaultCredentials
}

const actions: ActionTree<ApiCredentialsState, RootState> = {
}

const mutations: MutationTree<ApiCredentialsState> = {
}

const getters: GetterTree<ApiCredentialsState, RootState> = {}

export const apiCredentialsStateModule: Module<ApiCredentialsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}