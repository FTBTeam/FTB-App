import { Module } from 'vuex';
import { RootState } from '@/types';
import { AuthProfile, CoreMutations, CoreState } from '@/modules/core/core.types';

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
    getActiveProfile: (state: CoreState): string | undefined => {
      return state.activeProfile;
    },
  },
  actions: {
    // TODO: add validation on profile
    addProfile: ({ commit, state }, profile: AuthProfile) => {
      commit(CoreMutations.ADD_PROFILE, profile);
    },
  },
  mutations: {
    /**
     * Push a profile onto the state for persistence
     */
    addProfile: (state: CoreState, profile: AuthProfile) => {
      if (!state.profiles) {
        state.profiles = [];
      }

      state.profiles.push(profile);
    },
  },
};
