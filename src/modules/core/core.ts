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
  },
  actions: {
    addProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.ADD_PROFILE, profile);
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
              } as AuthProfile),
          );

          commit(CoreMutations.SET_ACTIVE_PROFILE, e.activeProfile);
          commit(CoreMutations.LOAD_PROFILES, profiles);
        },
      });
    },
  },
  mutations: {
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

    setActiveProfile: (state: CoreState, profile: AuthProfile) => {
      state.activeProfile = profile;
    },
  },
};
