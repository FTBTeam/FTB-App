import { Module } from 'vuex';
import { RootState } from '@/types';
import { AuthProfile, CoreMutations, CoreState } from '@/modules/core/core.types';
import store from '../store';

/**
 * Vuex, the correct way.
 */
export const core: Module<CoreState, RootState> = {
  namespaced: true,
  state: {
    profiles: [],
    activeProfile: {} as AuthProfile,
    signInOpened: false,
    instanceLoading: false
  },
  getters: {
    /**
     * Get the profile from the active profile
     */
    getProfiles: (state: CoreState): AuthProfile[] => {
      return state.profiles;
    },

    /**
     * Get the active profile
     */
    getActiveProfile: (state: CoreState): AuthProfile | undefined => {
      return state.activeProfile;
    },

    /**
     * Sign in opened?
     */
    getSignInOpened: (state: CoreState): boolean => {
      return state.signInOpened;
    },

    /**
     * Instance loading?
     */
     getInstanceLoading: (state: CoreState): boolean => {
      return state.instanceLoading;
    },
  },
  actions: {
    openSignIn: ({ commit }: { commit: any }) => {
      commit(CoreMutations.OPEN_SIGNIN, true);
    },

    closeSignIn: ({ commit }: { commit: any }) => {
      commit(CoreMutations.OPEN_SIGNIN, false);
    },

    addProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.ADD_PROFILE, profile);
    },

    removeProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.REMOVE_PROFILE, profile);
    },

    loadProfiles: ({ commit }) => {
      store.dispatch('sendMessage', {
        payload: {
          type: 'profiles.get',
        },
        callback: (e: any) => {
          const profiles = e.profiles.map(
            (a: any) =>
              ({
                type: a.isMicrosoft ? 'microsoft' : 'mojang',
                tokens: a.isMicrosoft
                  ? {
                      accessToken: a.msAuth.minecraftToken,
                      refreshToken: a.msAuth.liveRefreshToken,
                    }
                  : {
                      clientToken: a.mcAuth.clientToken,
                      accessToken: a.mcAuth.accessToken,
                    },
                username: a.username,
                uuid: a.uuid,
                expiresAt: a.isMicrosoft ? a.msAuth.liveExpiresAt : undefined
              } as AuthProfile),
          );

          commit(CoreMutations.SET_ACTIVE_PROFILE, e.activeProfile);
          commit(CoreMutations.LOAD_PROFILES, profiles);
        },
      });
    },
  },
  mutations: {
    openSignIn(state: CoreState, payload: boolean) {
      state.signInOpened = payload;
    },

    loadProfiles: (state: CoreState, profiles: AuthProfile[]) => {
      state.profiles = profiles;
    },

    /**
     * Push a profile onto the state for persistence
     */
    addProfile: (state: CoreState, profile: AuthProfile) => {
      if (!state.profiles) {
        state.profiles = [];
      }

      state.profiles.push(profile);
    },

    removeProfile: (state: CoreState, profile: AuthProfile) => {
      state.profiles = state.profiles.filter((p: AuthProfile) => p.uuid !== profile.uuid);
    },

    setActiveProfile: (state: CoreState, profile: AuthProfile) => {
      state.activeProfile = profile;
    },
  },
};
