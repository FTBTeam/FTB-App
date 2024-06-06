import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {MineTogetherAccount, MineTogetherProfile} from '@/core/@types/javaApi';

export type MTAuthState = typeof state;

const state = {
  profile: null as MineTogetherProfile | null,
  account: null as MineTogetherAccount | null
}

const actions: ActionTree<MTAuthState, RootState> = {
  setProfile({commit}, payload: MineTogetherProfile) {
    commit('SET_PROFILE', payload);
  },
  setAccount({commit}, payload: MineTogetherAccount) {
    commit('SET_ACCOUNT', payload);
  }
}

const mutations: MutationTree<MTAuthState> = {
  SET_PROFILE(state, payload: MineTogetherProfile) {
    state.profile = payload;
  },
  SET_ACCOUNT(state, payload: MineTogetherAccount) {
    state.account = payload;
  }
}

const getters: GetterTree<MTAuthState, RootState> = {
  profile(state) {
    return state.profile;
  },
  account(state) {
    return state.account;
  }
}

export type SetProfileMethod = (payload: MineTogetherProfile) => Promise<void>;
export type SetAccountMethod = (payload: MineTogetherAccount) => Promise<void>;

export const mtAuthStateModule: Module<MTAuthState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}