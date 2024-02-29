import {ActionTree, GetterTree, Module, MutationTree} from 'vuex';
import {RootState} from '@/types';
import {createLogger} from '@/core/logger';
import {constants} from '@/core/constants';

export type ApiCredentialsState = typeof state;

const defaultCredentials = {
  apiUrl: constants.modpacksApi,
  apiSecret: null as string | null,
  supportsPublicPrefix: true,
  usesBearerAuth: false,
  wasUserSet: false
}

const state = {
  ...defaultCredentials
}

export type ApiCredentialsPayload = Pick<ApiCredentialsState, "apiUrl" | "apiSecret" | "usesBearerAuth" | "supportsPublicPrefix">

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
  STORE_CREDENTIALS(state, payload: Partial<ApiCredentialsState>) {
    state.apiUrl = payload.apiUrl ?? state.apiUrl;
    state.apiSecret = payload.apiSecret ?? state.apiSecret;
    state.usesBearerAuth = payload.usesBearerAuth ?? state.usesBearerAuth;
    state.supportsPublicPrefix = payload.supportsPublicPrefix ?? state.supportsPublicPrefix;
  },
  RESET_CREDENTIALS(state) {
    state.apiUrl = defaultCredentials.apiUrl;
    state.apiSecret = defaultCredentials.apiSecret;
    state.usesBearerAuth = defaultCredentials.usesBearerAuth;
    state.supportsPublicPrefix = defaultCredentials.supportsPublicPrefix;
    state.wasUserSet = false;
  },
  SET_WAS_USER_SET(state, payload: boolean) {
    state.wasUserSet = payload;
  }
}

const getters: GetterTree<ApiCredentialsState, RootState> = {}

export const apiCredentialsStateModule: Module<ApiCredentialsState, RootState> = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}