import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {createLogger} from '@/core/logger';
import {constants} from '@/core/constants';

export type ApiCredentialsState = typeof state;

const defaultCredentials = {
  apiUrl: constants.modpacksApi,
  apiSecret: null as string | null,
  settings: {
    useAuthorizationHeader: false,
    useAuthorizationAsBearer: false,
    usePublicUrl: true
  },
  wasUserSet: false
}

const state = {
  ...defaultCredentials
}

export type ApiCredentialsPayload = Pick<ApiCredentialsState, "apiUrl" | "apiSecret" | "settings">

const logger = createLogger("apiCredentialsState.ts");

const actions: ActionTree<ApiCredentialsState, RootState> = {
  storeCredentials({commit, state}, payload: Partial<ApiCredentialsPayload>) {
    commit('STORE_CREDENTIALS', {
      ...state,
      ...payload,
    });
  },
  resetCredentials({commit}) {
    commit('RESET_CREDENTIALS');
  },
  setWasUserSet({commit}) {
    commit('SET_WAS_USER_SET', true);
  }
}

export type StoreCredentialsAction = (payload: Partial<ApiCredentialsPayload>) => Promise<void>;

const mutations: MutationTree<ApiCredentialsState> = {
  STORE_CREDENTIALS(state, payload: ApiCredentialsState) {
    state.apiUrl = payload.apiUrl;
    state.apiSecret = payload.apiSecret;
    state.settings = payload.settings;
  },
  RESET_CREDENTIALS(state) {
    state.apiUrl = defaultCredentials.apiUrl;
    state.apiSecret = defaultCredentials.apiSecret;
    state.settings = defaultCredentials.settings;
    state.wasUserSet = false;
  },
  SET_WAS_USER_SET(state, payload: boolean) {
    state.wasUserSet = payload;
  }
}

const getters: GetterTree<ApiCredentialsState, RootState> = {
  apiUrl(state) {
    return state.apiUrl;
  },
  apiSecret(state) {
    return state.apiSecret;
  },
  settings(state) {
    return state.settings;
  },
  wasUserSet(state) {
    return state.wasUserSet;
  }
}

export const apiCredentialsStateModule: Module<ApiCredentialsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}